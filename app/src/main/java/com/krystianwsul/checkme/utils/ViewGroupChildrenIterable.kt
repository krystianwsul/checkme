package com.krystianwsul.checkme.utils

import android.view.View
import android.view.ViewGroup

class ViewGroupChildrenIterable(private val viewGroup: ViewGroup) : Iterable<View> {

    override fun iterator(): Iterator<View> {
        val size = viewGroup.childCount
        var i = 0

        return object : Iterator<View> {

            override fun hasNext() = i < size

            override fun next() = viewGroup.getChildAt(i++)
        }
    }
}