package com.krystianwsul.checkme

import android.content.Context
import com.skydoves.balloon.Balloon
import com.skydoves.balloon.BalloonAnimation
import com.skydoves.balloon.createBalloon

object TooltipManager {

    private var tooltipVisible = false

    fun tryCreateBalloon(context: Context, type: Type, block: Balloon.Builder.() -> Unit): Balloon? {
        if (tooltipVisible) return null
        if (Preferences.getTooltipShown(type)) return null

        tooltipVisible = true
        Preferences.setTooltipShown(type)

        return createBalloon(context) {
            block()

            dismissWhenClicked = true
            dismissWhenTouchOutside = true

            setPaddingTop(8)
            setPaddingBottom(8)
            setPaddingLeft(16)
            setPaddingRight(16)

            setBalloonAnimation(BalloonAnimation.FADE)

            setOnBalloonDismissListener {
                check(tooltipVisible)

                tooltipVisible = false
            }
        }
    }

    enum class Type {

        PRESS_TO_SELECT,
        PRESS_MENU_TOOLTIP
    }
}