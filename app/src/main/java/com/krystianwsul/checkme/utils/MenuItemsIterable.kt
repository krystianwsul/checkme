package com.krystianwsul.checkme.utils

import android.view.Menu
import android.view.MenuItem

class MenuItemsIterable(private val menu: Menu) : Iterable<MenuItem> {

    override fun iterator(): Iterator<MenuItem> {
        val size = menu.size()
        var i = 0

        return object : Iterator<MenuItem> {

            override fun hasNext() = i < size

            override fun next() = menu.getItem(i++)
        }
    }
}