package com.krystianwsul.checkme.utils

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.view.View

fun animateVisibility(show: View, hide: View, duration: Int? = null) {
    animateVisibility(listOf(show), listOf(hide), duration)
}

fun animateVisibility(show: List<View>, hide: List<View>, duration: Int? = null) {
    val showPairs = show.map { Pair(it, HideType.GONE) }
    val hidePairs = hide.map { Pair(it, HideType.GONE) }

    animateVisibility2(showPairs, hidePairs, duration)
}

fun animateVisibility2(show: List<Pair<View, HideType>>, hide: List<Pair<View, HideType>>, duration: Int? = null) {
    val context = (show.firstOrNull() ?: hide.firstOrNull())!!.first.context

    val shortAnimTime = duration
            ?: context.resources.getInteger(android.R.integer.config_longAnimTime)

    for ((view, hideType) in show) {
        view.run {
            resetAlpha()

            if (hideType == HideType.GONE) {
                check(visibility != View.INVISIBLE)
            } else {
                check(hideType == HideType.INVISIBLE)
                check(visibility != View.GONE)
            }

            if (visibility == View.VISIBLE)
                return@run

            visibility = View.VISIBLE
            alpha = 0f

            animate().setDuration(shortAnimTime.toLong())
                    .alpha(1f)
                    .setListener(object : AnimatorListenerAdapter() {

                        override fun onAnimationEnd(animation: Animator) {
                            visibility = View.VISIBLE
                        }
                    })
        }
    }

    for ((view, hideType) in hide) {
        view.run {
            resetAlpha()

            if (hideType == HideType.GONE) {
                check(visibility != View.INVISIBLE)

                if (view.visibility == View.GONE)
                    return@run
            } else {
                check(hideType == HideType.INVISIBLE)
                check(visibility != View.GONE)

                if (visibility == View.INVISIBLE)
                    return@run
            }

            visibility = View.VISIBLE
            alpha = 1f

            animate().setDuration(shortAnimTime.toLong())
                    .alpha(0f)
                    .setListener(object : AnimatorListenerAdapter() {

                        override fun onAnimationEnd(animation: Animator) {
                            visibility = if (hideType == HideType.GONE) View.GONE else View.INVISIBLE
                            alpha = 1f
                        }
                    })
        }
    }
}

fun View.resetAlpha() {
    clearAnimation()
    animate().cancel()
    alpha = 1f
}

enum class HideType {
    GONE,
    INVISIBLE
}