package com.krystianwsul.checkme.gui.widgets

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.bottomappbar.BottomAppBar
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.utils.animateItems
import com.krystianwsul.checkme.utils.items

class MyBottomBar @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : BottomAppBar(context, attrs, defStyleAttr) {

    companion object {

        val duration by lazy {
            MyApplication.instance
                    .resources
                    .getInteger(android.R.integer.config_shortAnimTime)
        }
    }

    fun animateReplaceMenu(newMenu: Int, itemVisibilities: List<Pair<Int, Boolean>>? = null, onEnd: () -> Unit) {
        val visibleViews = itemVisibilities ?: menu.items.map { it.itemId to false }

        animateItems(visibleViews, true) {
            replaceMenu(newMenu)

            //val visible = menu.items.filter { it.isVisible }
            //visible.forEach { it.isVisible = false }

            //animateItems(visible.map { it.itemId to true }, onEnd)

            onEnd()
        }
    }
}