package com.lastwave.app.ui.common

import org.junit.Assert.assertEquals
import org.junit.Test

class TrackStepDirectionTest {

    @Test
    fun returnsZeroOnInitialOrSameTrack() {
        assertEquals(0, computeTrackStepDirection(null, -1, -1, "a", 0, 5))
        assertEquals(0, computeTrackStepDirection("a", 0, 5, "a", 0, 5))
    }

    @Test
    fun returnsZeroForInvalidIndicesOrSingleTrackQueue() {
        assertEquals(0, computeTrackStepDirection("a", -1, 3, "b", 1, 3))
        assertEquals(0, computeTrackStepDirection("a", 0, 3, "b", -1, 3))
        assertEquals(0, computeTrackStepDirection("a", 0, 1, "b", 0, 1))
        assertEquals(0, computeTrackStepDirection("a", 0, 3, "b", 1, 4))
    }

    @Test
    fun twoTrackQueueDoesNotInvertNextAndPrevious() {
        assertEquals(1, computeTrackStepDirection("a", 0, 2, "b", 1, 2))
        assertEquals(-1, computeTrackStepDirection("b", 1, 2, "a", 0, 2))
    }

    @Test
    fun threeOrMoreTracksSupportSequentialAndWrapAroundSteps() {
        assertEquals(1, computeTrackStepDirection("a", 0, 3, "b", 1, 3))
        assertEquals(1, computeTrackStepDirection("b", 1, 3, "c", 2, 3))
        assertEquals(-1, computeTrackStepDirection("c", 2, 3, "b", 1, 3))
        assertEquals(-1, computeTrackStepDirection("b", 1, 3, "a", 0, 3))

        // Wrap-around: last -> first is Next (+1), first -> last is Previous (-1).
        assertEquals(1, computeTrackStepDirection("c", 2, 3, "a", 0, 3))
        assertEquals(-1, computeTrackStepDirection("a", 0, 3, "c", 2, 3))
    }
}
