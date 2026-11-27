#pragma once

#include <array>
#include <cstddef>
#include <cstdint>

namespace lastwave::audio {

/**
 * Real-time 2048-point STFT phase-aware center vocal canceller (75% Hann OLA).
 * Provides instant fallback separation while the background MDX-Net AI model
 * downloads or warms up. Allocation-free in process().
 */
class KaraokeStftProcessor final {
public:
    static constexpr std::size_t kFftSize = 2048U;
    static constexpr std::size_t kHopSize = 512U;
    static constexpr std::size_t kNumBins = kFftSize / 2U + 1U;
    static constexpr std::size_t kRingSize = 4096U;

    KaraokeStftProcessor() noexcept = default;

    void configure(double sampleRate) noexcept;
    void reset() noexcept;

    /**
     * Processes interleaved stereo float samples in place.
     * vocalLevel: 0.0 = 100% vocal removal, 1.0 = original voice preserved.
     * wet: 0.0..1.0 smoothed enable/bypass crossfade.
     */
    void process(
        float* interleavedStereo,
        std::int32_t frameCount,
        float vocalLevel,
        float wet) noexcept;

private:
    struct Complex final {
        float re{0.0F};
        float im{0.0F};
    };

    void fft(std::array<Complex, kFftSize>& data, bool inverse) const noexcept;
    void processFrame(float vocalLevel) noexcept;

    double sampleRate_{48000.0};
    bool tablesReady_{false};

    std::array<float, kFftSize> hannWindow_{};
    std::array<std::uint16_t, kFftSize> bitRev_{};
    std::array<Complex, kFftSize / 2U> twiddlesFwd_{};
    std::array<Complex, kFftSize / 2U> twiddlesInv_{};

    // Circular stereo input & OLA synthesis buffers
    std::array<float, kRingSize> inLeft_{};
    std::array<float, kRingSize> inRight_{};
    std::array<float, kRingSize> outLeft_{};
    std::array<float, kRingSize> outRight_{};
    std::size_t inWritePos_{0U};
    std::size_t outReadPos_{0U};
    std::size_t hopCountdown_{kHopSize};

    // Reusable per-frame FFT scratch (audio thread only)
    std::array<Complex, kFftSize> fftLeft_{};
    std::array<Complex, kFftSize> fftRight_{};
    std::array<float, kNumBins> prevMidMag_{};
};

}  // namespace lastwave::audio
