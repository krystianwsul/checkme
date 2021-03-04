package com.krystianwsul.checkme.gui.utils

import android.view.View

fun View.measureVisibleHeight(visibleWidth: Int): Int {
    val widthSpec = View.MeasureSpec.makeMeasureSpec(
            visibleWidth,
            View.MeasureSpec.EXACTLY
    )
    val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
    measure(widthSpec, heightSpec)

    return measuredHeight
}

fun <T, U> List<Map<T, U>>.flatten(): Map<T, U> = flatMap { it.entries.map { it.key to it.value } }.toMap()