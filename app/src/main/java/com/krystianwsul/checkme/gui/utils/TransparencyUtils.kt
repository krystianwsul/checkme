package com.krystianwsul.checkme.gui.utils

import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowInsetsController
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.doOnLayout
import com.krystianwsul.checkme.R

fun setNavBarTransparency(window: Window, landscape: Boolean) {
    if (!landscape) {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        window.navigationBarColor = ContextCompat.getColor(window.context, R.color.primaryColor12)
    }
}

fun setNavBarColor(window: Window, rootView: View, landscape: Boolean) {
    if (!landscape) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { // todo check on emulator v30
            rootView.doOnLayout {
                window.insetsController!!.setSystemBarsAppearance(
                        WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                        WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                )
            }
        } else if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            @Suppress("DEPRECATION")
            window.decorView.apply {
                systemUiVisibility = systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            }
        }
    }
}