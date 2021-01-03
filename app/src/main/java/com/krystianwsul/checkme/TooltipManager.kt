package com.krystianwsul.checkme

import android.content.Context
import android.util.Log
import com.krystianwsul.checkme.utils.dpToPx
import com.skydoves.balloon.Balloon
import com.skydoves.balloon.BalloonAnimation
import com.skydoves.balloon.createBalloon
import kotlin.math.roundToInt

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

            setHeight(context.dpToPx(24).roundToInt())
            setPaddingLeft(context.dpToPx(8).roundToInt())
            setPaddingRight(context.dpToPx(8).roundToInt())

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