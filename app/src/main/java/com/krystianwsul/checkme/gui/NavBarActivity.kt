package com.krystianwsul.checkme.gui

import android.os.Bundle
import com.krystianwsul.checkme.gui.utils.setNavBarTransparency
import com.krystianwsul.checkme.utils.isLandscape

abstract class NavBarActivity : AbstractActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        window.setNavBarTransparency(resources.isLandscape)

        super.onCreate(savedInstanceState)
    }
}