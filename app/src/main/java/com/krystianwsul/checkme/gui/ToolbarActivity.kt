package com.krystianwsul.checkme.gui

import android.graphics.Color

abstract class ToolbarActivity : AbstractActivity() {

    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)

        window.statusBarColor = Color.TRANSPARENT
    }
}