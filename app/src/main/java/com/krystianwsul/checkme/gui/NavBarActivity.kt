package com.krystianwsul.checkme.gui

import android.os.Bundle
import com.krystianwsul.checkme.utils.isLandscape
import com.krystianwsul.checkme.utils.setTransparentNavigation

abstract class NavBarActivity : AbstractActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        window.setTransparentNavigation(resources.isLandscape)

        super.onCreate(savedInstanceState)
    }
}