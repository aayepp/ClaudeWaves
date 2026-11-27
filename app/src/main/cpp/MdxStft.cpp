#include "MdxStft.h"

#include <algorithm>
#include <array>
#include <cmath>
#include <cstdint>
#include <mutex>
#include <vector>

namespace lastwave::audio {
namespace {

constexpr std::size_t kRadix5 = 5U;
constexpr std::size_t kSubFft = 1024U;  // 5 * 1024 = 5120 = kMdxNfft
constexpr std::size_t kSubBits = 10U;
constexpr std::size_t kPaddedSamples = kMdxChunkSize + kMdxNfft;  // center=True adds kMdxNfft/2 on each end
constexpr double kTwoPi = 6.283185307179586476925286766559;

struct Complex final {
    float re{0.0F};
    float im{0.0F};
};

struct MdxTables final {
    std::array<float, kMdxNfft> hann{};
    std::array<float, kMdxNfft> hannSq{};
    std::array<std::uint16_t, kSubFft> bitRev1024{};
    std::array<Complex, kSubFft / 2U> tw1024Fwd{};
    std::array<Complex, kSubFft / 2U> tw1024Inv{};
    // Mixed-radix 5 x 1024 twiddles: W_5120^(n0 * k1) for n0=0..4, k1=0..1023
    std::array<Complex, kRadix5 * kSubFft> twMixFwd{};
    std::array<Complex, kRadix5 * kSubFft> twMixInv{};
    // 5-point DFT twiddles: W_5^(n0 * k0) for n0=0..4, k0=0..4
    std::array<Complex, kRadix5 * kRadix5> tw5Fwd{};
    std::array<Complex, kRadix5 * kRadix5> tw5Inv{};

    // Reusable work buffers guarded by table/work mutex to keep STFT/iSTFT fast
    std::array<Complex, kMdxNfft> fftInOut{};
    std::array<Complex, kRadix5 * kSubFft> subBuf{};
    std::vector<float> accL{};
    std::vector<float> accR{};
    std::vector<float> winSq{};
};

MdxTables& getTables() {
    static MdxTables tables = [] {
        MdxTables t{};
        for (std::size_t n = 0U; n < kMdxNfft; ++n) {
            const double angle = kTwoPi * static_cast<double>(n) / static_cast<double>(kMdxNfft);
            const float w = static_cast<float>(0.5 * (1.0 - std::cos(angle)));
            t.hann[n] = w;
            t.hannSq[n] = w * w;
        }
        for (std::size_t i = 0U; i < kSubFft; ++i) {
            std::size_t rev = 0U;
            for (std::size_t b = 0U; b < kSubBits; ++b) {
                rev = (rev << 1U) | ((i >> b) & 1U);
            }
            t.bitRev1024[i] = static_cast<std::uint16_t>(rev);
        }
        for (std::size_t k = 0U; k < kSubFft / 2U; ++k) {
            const double a = kTwoPi * static_cast<double>(k) / static_cast<double>(kSubFft);
            const float c = static_cast<float>(std::cos(a));
            const float s = static_cast<float>(std::sin(a));
            t.tw1024Fwd[k] = {c, -s};
            t.tw1024Inv[k] = {c, s};
        }
        for (std::size_t n0 = 0U; n0 < kRadix5; ++n0) {
            for (std::size_t k1 = 0U; k1 < kSubFft; ++k1) {
                const double a = kTwoPi * static_cast<double>(n0 * k1) / static_cast<double>(kMdxNfft);
                const float c = static_cast<float>(std::cos(a));
                const float s = static_cast<float>(std::sin(a));
                t.twMixFwd[n0 * kSubFft + k1] = {c, -s};
                t.twMixInv[n0 * kSubFft + k1] = {c, s};
            }
        }
        for (std::size_t k0 = 0U; k0 < kRadix5; ++k0) {
            for (std::size_t n0 = 0U; n0 < kRadix5; ++n0) {
                const double a = kTwoPi * static_cast<double>(n0 * k0) / static_cast<double>(kRadix5);
                const float c = static_cast<float>(std::cos(a));
                const float s = static_cast<float>(std::sin(a));
                t.tw5Fwd[k0 * kRadix5 + n0] = {c, -s};
                t.tw5Inv[k0 * kRadix5 + n0] = {c, s};
            }
        }
        t.accL.resize(kPaddedSamples, 0.0F);
        t.accR.resize(kPaddedSamples, 0.0F);
        t.winSq.resize(kPaddedSamples, 0.0F);
        return t;
    }();
    return tables;
}

std::mutex& mdxWorkMutex() {
    static std::mutex mutex;
    return mutex;
}

inline void fft1024(const MdxTables& t, Complex* data, bool inverse) noexcept {
    for (std::size_t i = 0U; i < kSubFft; ++i) {
        const std::size_t j = static_cast<std::size_t>(t.bitRev1024[i]);
        if (i < j) std::swap(data[i], data[j]);
    }
    const auto& tw = inverse ? t.tw1024Inv : t.tw1024Fwd;
    for (std::size_t half = 1U; half < kSubFft; half <<= 1U) {
        const std::size_t step = half << 1U;
        const std::size_t twStep = kSubFft / step;
        for (std::size_t blk = 0U; blk < kSubFft; blk += step) {
            for (std::size_t k = 0U; k < half; ++k) {
                const Complex w = tw[k * twStep];
                const std::size_t i0 = blk + k;
                const std::size_t i1 = i0 + half;
                const Complex a = data[i0];
                const Complex b = data[i1];
                const float tRe = b.re * w.re - b.im * w.im;
                const float tIm = b.re * w.im + b.im * w.re;
                data[i0] = {a.re + tRe, a.im + tIm};
                data[i1] = {a.re - tRe, a.im - tIm};
            }
        }
    }
}

void fft5120(MdxTables& t, bool inverse) noexcept {
    // 1. Deinterleave x[n1 * 5 + n0] into 5 contiguous 1024-pt transforms
    for (std::size_t n1 = 0U; n1 < kSubFft; ++n1) {
        const std::size_t base = n1 * kRadix5;
        for (std::size_t n0 = 0U; n0 < kRadix5; ++n0) {
            t.subBuf[n0 * kSubFft + n1] = t.fftInOut[base + n0];
        }
    }
    for (std::size_t n0 = 0U; n0 < kRadix5; ++n0) {
        fft1024(t, &t.subBuf[n0 * kSubFft], inverse);
    }
    // 2. Twiddle multiply + 5-point DFT into fftInOut[k0 * 1024 + k1]
    const auto& twMix = inverse ? t.twMixInv : t.twMixFwd;
    const auto& tw5 = inverse ? t.tw5Inv : t.tw5Fwd;
    for (std::size_t k1 = 0U; k1 < kSubFft; ++k1) {
        std::array<Complex, kRadix5> y{};
        y[0] = t.subBuf[k1];
        for (std::size_t n0 = 1U; n0 < kRadix5; ++n0) {
            const Complex v = t.subBuf[n0 * kSubFft + k1];
            const Complex w = twMix[n0 * kSubFft + k1];
            y[n0] = {v.re * w.re - v.im * w.im, v.re * w.im + v.im * w.re};
        }
        for (std::size_t k0 = 0U; k0 < kRadix5; ++k0) {
            float sumRe = y[0].re;
            float sumIm = y[0].im;
            for (std::size_t n0 = 1U; n0 < kRadix5; ++n0) {
                const Complex w5 = tw5[k0 * kRadix5 + n0];
                sumRe += y[n0].re * w5.re - y[n0].im * w5.im;
                sumIm += y[n0].re * w5.im + y[n0].im * w5.re;
            }
            if (inverse) {
                constexpr float invN = 1.0F / static_cast<float>(kMdxNfft);
                t.fftInOut[k0 * kSubFft + k1] = {sumRe * invN, sumIm * invN};
            } else {
                t.fftInOut[k0 * kSubFft + k1] = {sumRe, sumIm};
            }
        }
    }
}

inline std::size_t reflectIndex(std::int64_t idx, std::int64_t length) noexcept {
    if (idx < 0) idx = -idx;
    if (idx >= length) idx = 2 * length - 2 - idx;
    return static_cast<std::size_t>(std::clamp<std::int64_t>(idx, 0, length - 1));
}

}  // namespace

bool computeMdxStft(const float* interleavedStereo, float* outTensor) noexcept {
    if (interleavedStereo == nullptr || outTensor == nullptr) return false;
    std::lock_guard<std::mutex> lock(mdxWorkMutex());
    MdxTables& t = getTables();

    constexpr std::int64_t chunkLen = static_cast<std::int64_t>(kMdxChunkSize);
    for (std::size_t ch = 0U; ch < 2U; ++ch) {
        const std::size_t chReBase = (ch * 2U) * kMdxDimF * kMdxDimT;
        const std::size_t chImBase = (ch * 2U + 1U) * kMdxDimF * kMdxDimT;

        for (std::size_t frame = 0U; frame < kMdxDimT; ++frame) {
            const std::int64_t startIdx =
                static_cast<std::int64_t>(frame * kMdxHop) - static_cast<std::int64_t>(kMdxNfft / 2U);
            for (std::size_t n = 0U; n < kMdxNfft; ++n) {
                const std::size_t sIdx = reflectIndex(startIdx + static_cast<std::int64_t>(n), chunkLen);
                const float sample = interleavedStereo[sIdx * 2U + ch];
                t.fftInOut[n] = {sample * t.hann[n], 0.0F};
            }
            fft5120(t, false);

            // Match mdx_separator.py: zero bins 0..2 and keep bins 0..kMdxDimF-1
            for (std::size_t f = 0U; f < 3U; ++f) {
                outTensor[chReBase + f * kMdxDimT + frame] = 0.0F;
                outTensor[chImBase + f * kMdxDimT + frame] = 0.0F;
            }
            for (std::size_t f = 3U; f < kMdxDimF; ++f) {
                const Complex bin = t.fftInOut[f];
                outTensor[chReBase + f * kMdxDimT + frame] = bin.re;
                outTensor[chImBase + f * kMdxDimT + frame] = bin.im;
            }
        }
    }
    return true;
}

bool computeMdxIstft(
    const float* inTensor,
    float* outInterleavedGen,
    float compensate) noexcept {
    if (inTensor == nullptr || outInterleavedGen == nullptr) return false;
    std::lock_guard<std::mutex> lock(mdxWorkMutex());
    MdxTables& t = getTables();

    std::fill(t.accL.begin(), t.accL.end(), 0.0F);
    std::fill(t.accR.begin(), t.accR.end(), 0.0F);
    std::fill(t.winSq.begin(), t.winSq.end(), 0.0F);

    for (std::size_t ch = 0U; ch < 2U; ++ch) {
        const std::size_t chReBase = (ch * 2U) * kMdxDimF * kMdxDimT;
        const std::size_t chImBase = (ch * 2U + 1U) * kMdxDimF * kMdxDimT;
        float* acc = (ch == 0U) ? t.accL.data() : t.accR.data();

        for (std::size_t frame = 0U; frame < kMdxDimT; ++frame) {
            for (std::size_t f = 0U; f < kMdxDimF; ++f) {
                const float re = inTensor[chReBase + f * kMdxDimT + frame];
                const float im = inTensor[chImBase + f * kMdxDimT + frame];
                t.fftInOut[f] = {re, im};
                if (f > 0U) {
                    t.fftInOut[kMdxNfft - f] = {re, -im};
                }
            }
            // Bin 2560 (Nyquist = kMdxNfft / 2) was truncated by dim_f = 2560 -> zero-padded
            t.fftInOut[kMdxDimF] = {0.0F, 0.0F};

            fft5120(t, true);

            const std::size_t base = frame * kMdxHop;
            if (ch == 0U) {
                for (std::size_t n = 0U; n < kMdxNfft; ++n) {
                    acc[base + n] += t.fftInOut[n].re * t.hann[n];
                    t.winSq[base + n] += t.hannSq[n];
                }
            } else {
                for (std::size_t n = 0U; n < kMdxNfft; ++n) {
                    acc[base + n] += t.fftInOut[n].re * t.hann[n];
                }
            }
        }
    }

    // Center trim (kMdxNfft / 2 = 2560) + UVR overlap trim (kMdxTrim = 2560) = 5120
    constexpr std::size_t kCombinedOffset = (kMdxNfft / 2U) + kMdxTrim;
    const float safeComp = std::isfinite(compensate) ? compensate : 1.0F;
    for (std::size_t i = 0U; i < kMdxGenSize; ++i) {
        const std::size_t idx = kCombinedOffset + i;
        const float denom = t.winSq[idx];
        const float inv = (denom > 1.0e-8F) ? (safeComp / denom) : 0.0F;
        outInterleavedGen[i * 2U] = std::clamp(t.accL[idx] * inv, -1.0F, 1.0F);
        outInterleavedGen[i * 2U + 1U] = std::clamp(t.accR[idx] * inv, -1.0F, 1.0F);
    }
    return true;
}

}  // namespace lastwave::audio
