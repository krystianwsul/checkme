package com.krystianwsul.checkme.utils

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.view.View
import android.view.ViewPropertyAnimator

fun animateVisibility(show: View, hide: View, duration: Int? = null) = animateVisibility(listOf(show), listOf(hide), duration)

fun animateVisibility(show: List<View> = listOf(), hide: List<View> = listOf(), duration: Int? = null, immediate: Boolean = false, onEnd: (() -> Unit)? = null) {
    val showPairs = show.map { Pair(it, HideType.GONE) }
    val hidePairs = hide.map { Pair(it, HideType.GONE) }

    animateVisibility2(showPairs, hidePairs, duration, immediate, onEnd)
}

fun animateVisibility2(show: List<Pair<View, HideType>>, hide: List<Pair<View, HideType>>, duration: Int? = null, immediate: Boolean = false, onEnd: (() -> Unit)? = null) {
    if (show.isEmpty() && hide.isEmpty()) {
        onEnd?.invoke()
        return
    }

    val context = (show.firstOrNull() ?: hide.firstOrNull())!!.first.context

    val shortAnimTime = duration
            ?: context.resources.getInteger(android.R.integer.config_longAnimTime)

    var showWaiting = show.size
    var hideWaiting = hide.size

    fun callOnEnd() {
        if (showWaiting == 0 && hideWaiting == 0)
            onEnd?.invoke()
    }

    for ((view, hideType) in show) {
        view.run {
            resetAlpha()

            check(visibility != hideType.opposite)

            if (visibility == View.VISIBLE) {
                showWaiting--
                callOnEnd()

                return@run
            }

            visibility = View.VISIBLE

            if (immediate) {
                showWaiting--
                callOnEnd()
            } else {
                alpha = 0f

                animate().setDuration(shortAnimTime.toLong())
                        .alpha(1f)
                        .onEnd {
                            visibility = View.VISIBLE

                            showWaiting--
                            callOnEnd()
                        }
            }
        }
    }

    for ((view, hideType) in hide) {
        view.run {
            resetAlpha()

            check(visibility != hideType.opposite)

            if (visibility == hideType.visibility) {
                hideWaiting--
                callOnEnd()

                return@run
            }

            if (immediate) {
                visibility = hideType.visibility

                hideWaiting--
                callOnEnd()
            } else {
                visibility = View.VISIBLE
                alpha = 1f

                animate().setDuration(shortAnimTime.toLong())
                        .alpha(0f)
                        .onEnd {
                            visibility = hideType.visibility
                            alpha = 1f

                            hideWaiting--
                            callOnEnd()
                        }
            }
        }
    }
}

private fun ViewPropertyAnimator.onEnd(action: () -> Unit) {
    setListener(object : AnimatorListenerAdapter() {

        override fun onAnimationEnd(animation: Animator?) = action()
    })
}

fun View.resetAlpha() {
    clearAnimation()
    animate().cancel()
    alpha = 1f
}

enum class HideType(val visibility: Int, val opposite: Int) {

    GONE(View.GONE, View.INVISIBLE),
    INVISIBLE(View.INVISIBLE, View.GONE)
}