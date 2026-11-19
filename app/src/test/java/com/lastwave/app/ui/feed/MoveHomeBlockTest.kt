package com.lastwave.app.ui.feed

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MoveHomeBlockTest {

    private val order = listOf(HomeBlock.QUICK_ACCESS, HomeBlock.MOOD_CHIPS, HomeBlock.TOP_CHARTS)

    @Test
    fun moveUp_skipsHiddenNeighbour() {
        val hidden = setOf(HomeBlock.MOOD_CHIPS.id)
        assertThat(moveHomeBlock(order, hidden, HomeBlock.TOP_CHARTS, up = true))
            .containsExactly(HomeBlock.TOP_CHARTS, HomeBlock.MOOD_CHIPS, HomeBlock.QUICK_ACCESS).inOrder()
    }

    @Test
    fun moveDown_atLastVisible_isUnchanged() {
        assertThat(moveHomeBlock(order, emptySet(), HomeBlock.TOP_CHARTS, up = false)).isEqualTo(order)
    }
}
