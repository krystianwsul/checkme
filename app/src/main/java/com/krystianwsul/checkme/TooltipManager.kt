package com.krystianwsul.checkme

import android.content.Context
import androidx.annotation.CheckResult
import com.skydoves.balloon.Balloon
import com.skydoves.balloon.BalloonAnimation
import com.skydoves.balloon.createBalloon
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import java.util.concurrent.TimeUnit

object TooltipManager {

    private var tooltipVisible = false

    fun tryCreateBalloon(
            context: Context,
            type: Type,
            block: Balloon.Builder.() -> Unit,
            show: Balloon.() -> Unit,
    ): Balloon? {
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
        }.apply(show)
    }

    @CheckResult
    fun <T> Observable<T>.subscribeGetBalloon(showBalloon: (T) -> Balloon?): Disposable {
        var balloon: Balloon? = null

        return doOnDispose { balloon?.dismiss() }.subscribe { balloon = showBalloon(it) }
    }

    @CheckResult
    fun Observable<*>.subscribeShowBalloon(
            context: Context,
            type: Type,
            block: Balloon.Builder.() -> Unit,
            show: Balloon.() -> Unit,
    ): Disposable {
        return subscribeGetBalloon { tryCreateBalloon(context, type, block, show) }
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
        PRESS_DRAG
    }
}