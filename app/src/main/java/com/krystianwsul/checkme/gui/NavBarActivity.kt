package com.krystianwsul.checkme.gui

import android.os.Bundle
import android.view.View
import com.krystianwsul.checkme.gui.utils.setNavBarColor
import com.krystianwsul.checkme.gui.utils.setNavBarTransparency
import com.krystianwsul.checkme.utils.isLandscape

abstract class NavBarActivity : AbstractActivity() {

    protected abstract val rootView: View

    override fun onCreate(savedInstanceState: Bundle?) {
        setNavBarTransparency(window, resources.isLandscape)

        super.onCreate(savedInstanceState)
    }

    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)

        setNavBarColor(window, rootView, resources.isLandscape)
    }
}