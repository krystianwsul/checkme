package com.krystianwsul.checkme.gui.utils

import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowInsetsController
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.krystianwsul.checkme.R

fun Window.setNavBarTransparency(landscape: Boolean) {
    if (!landscape) {
        WindowCompat.setDecorFitsSystemWindows(this, false)

        navigationBarColor = ContextCompat.getColor(context, R.color.primaryColor12)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            insetsController!!.setSystemBarsAppearance(
                    WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                    WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
            )
        } else if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            @Suppress("DEPRECATION")
            decorView.apply {
                systemUiVisibility = systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            }
        }
    }
}
