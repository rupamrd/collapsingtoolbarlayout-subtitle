package com.hendraanggrian.collapsingtoolbarlayout.subtitle

import android.os.SystemClock
import android.support.test.espresso.UiController
import android.support.test.espresso.action.MotionEvents
import android.support.test.espresso.action.Swiper
import android.support.test.espresso.action.Swiper.Status.FAILURE
import android.support.test.espresso.action.Swiper.Status.SUCCESS
import android.util.Log
import com.google.common.base.Preconditions.checkElementIndex

/**
 * @see android.support.test.espresso.action.Swipe
 */
internal class SlowerSwipe : Swiper {

    override fun sendSwipe(uiController: UiController, startCoordinates: FloatArray, endCoordinates: FloatArray, precision: FloatArray): Swiper.Status =
        sendLinearSwipe(uiController, startCoordinates, endCoordinates, precision, SWIPE_SLOWER_DURATION_MS)

    private companion object {
        const val SWIPE_EVENT_COUNT = 10
        const val SWIPE_SLOWER_DURATION_MS = 5000

        fun sendLinearSwipe(uiController: UiController, startCoordinates: FloatArray, endCoordinates: FloatArray, precision: FloatArray, duration: Int): Swiper.Status {
            checkNotNull(uiController)
            checkNotNull(startCoordinates)
            checkNotNull(endCoordinates)
            checkNotNull(precision)
            val steps = interpolate(startCoordinates, endCoordinates, SWIPE_EVENT_COUNT)
            val delayBetweenMovements = duration / steps.size
            val downEvent = MotionEvents.sendDown(uiController, startCoordinates, precision).down
            try {
                for (i in steps.indices) {
                    if (!MotionEvents.sendMovement(uiController, downEvent, steps[i])) {
                        Log.e("SlowerSwipe", "Injection of move event as part of the swipe failed. Sending cancel event.")
                        MotionEvents.sendCancel(uiController, downEvent)
                        return FAILURE
                    }

                    val desiredTime = downEvent.downTime + delayBetweenMovements * i
                    val timeUntilDesired = desiredTime - SystemClock.uptimeMillis()
                    if (timeUntilDesired > 10) {
                        uiController.loopMainThreadForAtLeast(timeUntilDesired)
                    }
                }
                if (!MotionEvents.sendUp(uiController, downEvent, endCoordinates)) {
                    Log.e("SlowerSwipe", "Injection of up event as part of the swipe failed. Sending cancel event.")
                    MotionEvents.sendCancel(uiController, downEvent)
                    return FAILURE
                }
            } finally {
                downEvent.recycle()
            }
            return SUCCESS
        }

        fun interpolate(start: FloatArray, end: FloatArray, steps: Int): Array<FloatArray> {
            checkElementIndex(1, start.size)
            checkElementIndex(1, end.size)
            val res = Array(steps) { FloatArray(2) }
            for (i in 1 until steps + 1) {
                res[i - 1][0] = start[0] + (end[0] - start[0]) * i / (steps + 2f)
                res[i - 1][1] = start[1] + (end[1] - start[1]) * i / (steps + 2f)
            }
            return res
        }
    }
}