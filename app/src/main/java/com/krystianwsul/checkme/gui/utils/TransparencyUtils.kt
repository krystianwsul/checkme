package com.krystianwsul.checkme.gui.utils

import android.os.Build
import android.view.View
import android.view.Window
import androidx.core.content.ContextCompat
import com.krystianwsul.checkme.R

fun Window.setNavBarTransparency(landscape: Boolean) {
    if (!landscape) {
        var flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION

        navigationBarColor = ContextCompat.getColor(context, R.color.primaryColor12Solid)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            flags = flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR

        decorView.systemUiVisibility = decorView.systemUiVisibility or flags
    }
}