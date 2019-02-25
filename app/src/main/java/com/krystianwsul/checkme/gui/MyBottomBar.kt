package com.krystianwsul.checkme.gui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import com.google.android.material.bottomappbar.BottomAppBar
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.utils.animateVisibility
import com.krystianwsul.checkme.utils.items

class MyBottomBar @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : BottomAppBar(context, attrs, defStyleAttr) {

    companion object {

        private val duration = MyApplication.instance
                .resources
                .getInteger(android.R.integer.config_shortAnimTime)
    }

    fun animateReplaceMenu(newMenu: Int, onEnd: () -> Unit) {
        val visibleViews = menu.items
                .filter { it.isVisible }
                .mapNotNull { findViewById<View>(it.itemId) }

        animateVisibility(hide = visibleViews, duration = duration, onEnd = {
            replaceMenu(newMenu)
            onEnd()
        })
    }
}