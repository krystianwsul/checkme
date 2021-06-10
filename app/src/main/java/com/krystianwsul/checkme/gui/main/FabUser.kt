package com.krystianwsul.checkme.gui.main

import com.krystianwsul.checkme.gui.utils.BottomFabMenuDelegate

interface FabUser {

    fun setFab(fabDelegate: BottomFabMenuDelegate.FabDelegate)

    fun clearFab()
}
