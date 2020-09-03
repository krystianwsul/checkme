package com.krystianwsul.checkme.gui

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.utils.isLandscape

abstract class NavBarActivity : AbstractActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val landscape = resources.isLandscape

        window.apply {
            if (!landscape) {
                var flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    flags = flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR

                decorView.systemUiVisibility = decorView.systemUiVisibility or flags

                navigationBarColor = ContextCompat.getColor(context, R.color.primaryColor12Solid)
            }
        }

        super.onCreate(savedInstanceState)
    }
}