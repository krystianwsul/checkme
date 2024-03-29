package com.krystianwsul.checkme

import android.content.Context
import androidx.annotation.CheckResult
import com.skydoves.balloon.Balloon
import com.skydoves.balloon.BalloonAnimation
import com.skydoves.balloon.createBalloon
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import java.util.concurrent.TimeUnit

object TooltipManager {

    private var tooltipVisible = false

    private fun tryCreateBalloon(
            context: Context,
            type: Type,
            block: Balloon.Builder.() -> Unit,
            show: Balloon.() -> Unit,
    ): Balloon? {
        if (tooltipVisible) return null
        if (!type.canBeShown()) return null

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
        }.apply(show)
    }

    @CheckResult
    fun <T : Any> Observable<T>.subscribeShowBalloon(
            context: Context,
            type: Type,
            block: Balloon.Builder.(T) -> Unit,
            show: Balloon.(T) -> Unit,
    ): Disposable {
        var balloon: Balloon? = null

        return doOnDispose { balloon?.dismiss() }.subscribe {
            balloon = tryCreateBalloon(context, type, { block(it) }, { show(it) })
        }
    }

    @CheckResult
    fun fiveSecondDelay(): Observable<Unit> {
        return Observable.just(Unit)
                .mergeWith(Observable.never())
                .delay(5, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
    }

    enum class Type {

        PRESS_TO_SELECT,
        PRESS_MENU_TOOLTIP,
        PRESS_DRAG,
        TASKS_TAB,
        ADD_PROJECT {

            override val dependsOn = listOf(TASKS_TAB)
        };

        open val dependsOn = listOf<Type>()

        private fun hasBeenShown() = Preferences.getTooltipShown(this)

        fun canBeShown(): Boolean {
            if (dependsOn.any { !it.hasBeenShown() }) return false

            return !hasBeenShown()
        }
    }
}