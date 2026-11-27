#pragma once

#include <cstddef>

namespace lastwave::audio {

// Exact parameters for UVR-MDX-NET-Voc_FT (t128, verified against TRvlvr model_data_new.json):
// mdx_n_fft_scale_set = 7680, mdx_dim_f_set = 3072, mdx_dim_t_set = 128, compensate = 1.021
constexpr std::size_t kMdxNfft = 7680U;
constexpr std::size_t kMdxHop = 1024U;
constexpr std::size_t kMdxDimF = 3072U;
constexpr std::size_t kMdxDimT = 128U;
constexpr std::size_t kMdxTrim = 3840U;
constexpr std::size_t kMdxChunkSize = 130048U;  // kMdxHop * (kMdxDimT - 1)
constexpr std::size_t kMdxGenSize = 122368U;    // kMdxChunkSize - 2 * kMdxTrim
constexpr std::size_t kMdxTensorFloats = 4U * kMdxDimF * kMdxDimT;

/**
 * Computes PyTorch-equivalent STFT (n_fft=7680, hop=1024, periodic Hann, center=True)
 * for [interleavedStereo] (kMdxChunkSize * 2 floats), outputting [outTensor] of shape
 * [4, 3072, 128] (L_re, L_im, R_re, R_im) with bins 0..2 zeroed per UVR spec.
 */
bool computeMdxStft(const float* interleavedStereo, float* outTensor) noexcept;

/**
 * Computes PyTorch-equivalent iSTFT for [inTensor] ([4, 3072, 128]), trims center
 * padding + 3840-frame edges, scales by [compensate], and writes kMdxGenSize * 2
 * interleaved stereo floats to [outInterleavedGen].
 */
bool computeMdxIstft(
    const float* inTensor,
    float* outInterleavedGen,
    float compensate) noexcept;

}  // namespace lastwave::audio
