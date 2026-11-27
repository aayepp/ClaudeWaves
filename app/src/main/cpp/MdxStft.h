#pragma once

#include <cstddef>

namespace lastwave::audio {

// Exact parameters for UVR-MDX-NET-Inst_HQ_4.onnx (verified against TRvlvr model_data_new.json):
// mdx_n_fft_scale_set = 5120, mdx_dim_f_set = 2560, mdx_dim_t_set = 8 (2^8 = 256), compensate = 1.019
constexpr std::size_t kMdxNfft = 5120U;
constexpr std::size_t kMdxHop = 1024U;
constexpr std::size_t kMdxDimF = 2560U;
constexpr std::size_t kMdxDimT = 256U;
constexpr std::size_t kMdxTrim = 2560U;
constexpr std::size_t kMdxChunkSize = 261120U;  // kMdxHop * (kMdxDimT - 1)
constexpr std::size_t kMdxGenSize = 256000U;    // kMdxChunkSize - 2 * kMdxTrim
constexpr std::size_t kMdxTensorFloats = 4U * kMdxDimF * kMdxDimT;

/**
 * Computes PyTorch-equivalent STFT (n_fft=5120, hop=1024, periodic Hann, center=True)
 * for [interleavedStereo] (kMdxChunkSize * 2 floats), outputting [outTensor] of shape
 * [4, 2560, 256] (L_re, L_im, R_re, R_im) with bins 0..2 zeroed per UVR spec.
 */
bool computeMdxStft(const float* interleavedStereo, float* outTensor) noexcept;

/**
 * Computes PyTorch-equivalent iSTFT for [inTensor] ([4, 2560, 256]), trims center
 * padding + 2560-frame edges, scales by [compensate], and writes kMdxGenSize * 2
 * interleaved stereo floats to [outInterleavedGen].
 */
bool computeMdxIstft(
    const float* inTensor,
    float* outInterleavedGen,
    float compensate) noexcept;

}  // namespace lastwave::audio
