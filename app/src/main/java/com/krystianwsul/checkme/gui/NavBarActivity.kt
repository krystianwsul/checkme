package com.krystianwsul.checkme.gui

import android.view.View
import com.krystianwsul.checkme.gui.utils.setNavBarTransparency
import com.krystianwsul.checkme.utils.isLandscape

abstract class NavBarActivity : AbstractActivity() {

    protected abstract val rootView: View

    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)

        setNavBarTransparency(window, rootView, resources.isLandscape)
    }
}