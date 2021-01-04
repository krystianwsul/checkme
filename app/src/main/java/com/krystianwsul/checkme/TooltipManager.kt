package com.krystianwsul.checkme

import android.content.Context
import android.util.Log
import com.skydoves.balloon.Balloon
import com.skydoves.balloon.BalloonAnimation
import com.skydoves.balloon.createBalloon

object TooltipManager {

    private var tooltipVisible = false

    fun tryCreateBalloon(context: Context, type: Type, block: Balloon.Builder.() -> Unit): Balloon? {
        if (tooltipVisible) return null
        if (Preferences.getTooltipShown(type.name)) return null

        tooltipVisible = true
        //Preferences.setTooltipShown(type.name) todo tooltip

        return createBalloon(context) {
            block()

            dismissWhenClicked = true
            dismissWhenTouchOutside = true

            setHeight(32 + 23) // plus arrow
            setPaddingLeft(16)
            setPaddingRight(16)

            setBalloonAnimation(BalloonAnimation.FADE)

            setOnBalloonDismissListener {
                check(tooltipVisible)
                Log.e("asdf", "magic dismiss")

                tooltipVisible = false
            }
        }
    }

    enum class Type {

        PRESS_TO_SELECT
    }
}