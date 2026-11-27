#include "KaraokeStft.h"

#include <algorithm>
#include <cmath>

namespace lastwave::audio {
namespace {

constexpr double kTwoPi = 6.283185307179586476925286766559;
constexpr float kOlaNorm = 1.5F;  // Sum of periodic Hann^2 at 75% overlap (2048 / 512) * 0.375

inline float smoothStep(float edge0, float edge1, float x) noexcept {
    if (edge1 <= edge0) return x >= edge1 ? 1.0F : 0.0F;
    const float t = std::clamp((x - edge0) / (edge1 - edge0), 0.0F, 1.0F);
    return t * t * (3.0F - 2.0F * t);
}

}  // namespace

void KaraokeStftProcessor::configure(double sampleRate) noexcept {
    sampleRate_ = std::max(sampleRate, 8000.0);
    if (!tablesReady_) {
        for (std::size_t n = 0U; n < kFftSize; ++n) {
            const double angle = kTwoPi * static_cast<double>(n) / static_cast<double>(kFftSize);
            hannWindow_[n] = static_cast<float>(0.5 * (1.0 - std::cos(angle)));
        }
        constexpr std::size_t kBits = 11U;  // 2^11 = 2048
        for (std::size_t i = 0U; i < kFftSize; ++i) {
            std::size_t reversed = 0U;
            for (std::size_t b = 0U; b < kBits; ++b) {
                reversed = (reversed << 1U) | ((i >> b) & 1U);
            }
            bitRev_[i] = static_cast<std::uint16_t>(reversed);
        }
        for (std::size_t k = 0U; k < kFftSize / 2U; ++k) {
            const double angle = kTwoPi * static_cast<double>(k) / static_cast<double>(kFftSize);
            const float c = static_cast<float>(std::cos(angle));
            const float s = static_cast<float>(std::sin(angle));
            twiddlesFwd_[k] = {c, -s};
            twiddlesInv_[k] = {c, s};
        }
        tablesReady_ = true;
    }
    reset();
}

void KaraokeStftProcessor::reset() noexcept {
    inLeft_.fill(0.0F);
    inRight_.fill(0.0F);
    outLeft_.fill(0.0F);
    outRight_.fill(0.0F);
    prevMidMag_.fill(0.0F);
    inWritePos_ = 0U;
    // Align read pointer one hop ahead so zero-latency OLA accumulation fills properly.
    outReadPos_ = 0U;
    hopCountdown_ = kHopSize;
}

void KaraokeStftProcessor::fft(
    std::array<Complex, kFftSize>& data,
    bool inverse) const noexcept {
    for (std::size_t i = 0U; i < kFftSize; ++i) {
        const std::size_t j = static_cast<std::size_t>(bitRev_[i]);
        if (i < j) {
            std::swap(data[i], data[j]);
        }
    }
    const auto& twiddles = inverse ? twiddlesInv_ : twiddlesFwd_;
    for (std::size_t halfLen = 1U; halfLen < kFftSize; halfLen <<= 1U) {
        const std::size_t fullLen = halfLen << 1U;
        const std::size_t twStep = kFftSize / fullLen;
        for (std::size_t block = 0U; block < kFftSize; block += fullLen) {
            for (std::size_t k = 0U; k < halfLen; ++k) {
                const Complex w = twiddles[k * twStep];
                const std::size_t i0 = block + k;
                const std::size_t i1 = i0 + halfLen;
                const Complex a = data[i0];
                const Complex b = data[i1];
                const float tRe = b.re * w.re - b.im * w.im;
                const float tIm = b.re * w.im + b.im * w.re;
                data[i0] = {a.re + tRe, a.im + tIm};
                data[i1] = {a.re - tRe, a.im - tIm};
            }
        }
    }
    if (inverse) {
        constexpr float invN = 1.0F / static_cast<float>(kFftSize);
        for (auto& bin : data) {
            bin.re *= invN;
            bin.im *= invN;
        }
    }
}

void KaraokeStftProcessor::processFrame(float vocalLevel) noexcept {
    const float removal = std::clamp(1.0F - vocalLevel, 0.0F, 1.0F);
    const std::size_t frameStart = (inWritePos_ + kRingSize - kFftSize) & (kRingSize - 1U);
    for (std::size_t n = 0U; n < kFftSize; ++n) {
        const std::size_t idx = (frameStart + n) & (kRingSize - 1U);
        const float w = hannWindow_[n];
        fftLeft_[n] = {inLeft_[idx] * w, 0.0F};
        fftRight_[n] = {inRight_[idx] * w, 0.0F};
    }

    fft(fftLeft_, false);
    fft(fftRight_, false);

    // Measure positive spectral flux of centered energy across 150..8000 Hz so sharp
    // snare/kick transients ("rentak") pass through while sustained centered vocals stay damped.
    float totalMidMag = 0.0F;
    float totalPosFlux = 0.0F;
    for (std::size_t k = 0U; k < kNumBins; ++k) {
        const Complex cL = fftLeft_[k];
        const Complex cR = fftRight_[k];
        const float mRe = 0.5F * (cL.re + cR.re);
        const float mIm = 0.5F * (cL.im + cR.im);
        const float midMag = std::sqrt(mRe * mRe + mIm * mIm);
        const float flux = std::max(0.0F, midMag - prevMidMag_[k]);
        prevMidMag_[k] = midMag;
        const float freqHz = static_cast<float>(
            static_cast<double>(k) * sampleRate_ / static_cast<double>(kFftSize));
        if (freqHz >= 150.0F && freqHz <= 8000.0F) {
            totalMidMag += midMag;
            totalPosFlux += flux;
        }
    }
    const float transientRatio = totalPosFlux / (totalMidMag + 1.0e-6F);
    const float beatPreserve = 0.25F * smoothStep(0.42F, 0.68F, transientRatio);

    const float sideBoost = 1.0F + 0.14F * removal;
    for (std::size_t k = 0U; k < kNumBins; ++k) {
        const float freqHz = static_cast<float>(
            static_cast<double>(k) * sampleRate_ / static_cast<double>(kFftSize));

        // Keep 100% of sub-bass & kick fundamentals (<= 135 Hz) and hi-hat/cymbal sheen (> 7.5 kHz)
        float bandWeight = 1.0F;
        if (freqHz <= 135.0F) {
            bandWeight = 0.0F;
        } else if (freqHz < 240.0F) {
            bandWeight = smoothStep(135.0F, 240.0F, freqHz);
        } else if (freqHz > 7500.0F) {
            bandWeight = 1.0F - 0.72F * smoothStep(7500.0F, 12000.0F, freqHz);
        }

        const Complex cL = fftLeft_[k];
        const Complex cR = fftRight_[k];

        const float mRe = 0.5F * (cL.re + cR.re);
        const float mIm = 0.5F * (cL.im + cR.im);
        const float sRe = 0.5F * (cL.re - cR.re);
        const float sIm = 0.5F * (cL.im - cR.im);

        const float magM2 = mRe * mRe + mIm * mIm;
        const float magS2 = sRe * sRe + sIm * sIm;
        const float magL = std::sqrt(cL.re * cL.re + cL.im * cL.im);
        const float magR = std::sqrt(cR.re * cR.re + cR.im * cR.im);

        // Centered vocal energy dominance & phase coherence between L and R
        const float cRatio = magM2 / (magM2 + magS2 + 1.0e-12F);
        const float cosDelta = (cL.re * cR.re + cL.im * cR.im) / (magL * magR + 1.0e-12F);
        const float inPhase = std::max(0.0F, cosDelta);

        const float centerMask = std::clamp(std::pow(cRatio, 0.75F) * inPhase, 0.0F, 1.0F);
        const float vocalBandFocus = smoothStep(0.28F, 0.70F, cRatio) * inPhase;
        const float baseSuppression = std::clamp(
            0.82F * centerMask + 0.18F * vocalBandFocus, 0.0F, 0.985F);
        const float suppressionStrength = baseSuppression * (1.0F - beatPreserve);
        const float midKeep = 1.0F - removal * bandWeight * suppressionStrength;
        const float effectiveSideGain = 1.0F + (sideBoost - 1.0F) * bandWeight;

        const float outLRe = mRe * midKeep + sRe * effectiveSideGain;
        const float outLIm = mIm * midKeep + sIm * effectiveSideGain;
        const float outRRe = mRe * midKeep - sRe * effectiveSideGain;
        const float outRIm = mIm * midKeep - sIm * effectiveSideGain;

        fftLeft_[k] = {outLRe, outLIm};
        fftRight_[k] = {outRRe, outRIm};
        if (k > 0U && k < kNumBins - 1U) {
            const std::size_t mirror = kFftSize - k;
            fftLeft_[mirror] = {outLRe, -outLIm};
            fftRight_[mirror] = {outRRe, -outRIm};
        }
    }

    fft(fftLeft_, true);
    fft(fftRight_, true);

    const std::size_t olaStart = outReadPos_;
    for (std::size_t n = 0U; n < kFftSize; ++n) {
        const std::size_t idx = (olaStart + n) & (kRingSize - 1U);
        const float synGain = hannWindow_[n] / kOlaNorm;
        outLeft_[idx] += fftLeft_[n].re * synGain;
        outRight_[idx] += fftRight_[n].re * synGain;
    }
}

void KaraokeStftProcessor::process(
    float* interleavedStereo,
    std::int32_t frameCount,
    float vocalLevel,
    float wet) noexcept {
    if (interleavedStereo == nullptr || frameCount <= 0) return;
    if (!tablesReady_) configure(sampleRate_);

    const std::size_t count = static_cast<std::size_t>(frameCount);
    const float clampedWet = std::clamp(wet, 0.0F, 1.0F);
    const float clampedVocal = std::clamp(vocalLevel, 0.0F, 1.0F);

    for (std::size_t i = 0U; i < count; ++i) {
        const std::size_t off = i * 2U;
        const float dryL = interleavedStereo[off];
        const float dryR = interleavedStereo[off + 1U];

        inLeft_[inWritePos_] = dryL;
        inRight_[inWritePos_] = dryR;
        inWritePos_ = (inWritePos_ + 1U) & (kRingSize - 1U);

        if (--hopCountdown_ == 0U) {
            hopCountdown_ = kHopSize;
            processFrame(clampedVocal);
        }

        const float wetL = outLeft_[outReadPos_];
        const float wetR = outRight_[outReadPos_];
        outLeft_[outReadPos_] = 0.0F;
        outRight_[outReadPos_] = 0.0F;
        outReadPos_ = (outReadPos_ + 1U) & (kRingSize - 1U);

        interleavedStereo[off] = dryL + (wetL - dryL) * clampedWet;
        interleavedStereo[off + 1U] = dryR + (wetR - dryR) * clampedWet;
    }
}

}  // namespace lastwave::audio
