package com.lastwave.app.playback.karaoke

import com.lastwave.app.playback.NativeAudioEngine
import java.nio.ByteBuffer

internal object MdxStftNative {
    const val MDX_SAMPLE_RATE = 44_100
    const val MDX_HOP_SIZE = 1_024
    const val MDX_DIM_F = 3_072
    const val MDX_DIM_T = 128
    const val MDX_TRIM = 3_840
    const val MDX_CHUNK_SIZE = 130_048
    const val MDX_GEN_SIZE = 122_368
    const val MDX_TENSOR_FLOATS = 4 * MDX_DIM_F * MDX_DIM_T
    const val MDX_COMPENSATE = 1.021f

    val isAvailable: Boolean
        get() = NativeAudioEngine.libraryLoaded

    external fun nativeComputeStft(stereoInput: FloatArray, outTensorBuffer: ByteBuffer): Boolean

    external fun nativeComputeIstft(
        inTensorBuffer: ByteBuffer,
        stereoOutput: FloatArray,
        compensate: Float,
    ): Boolean
}
