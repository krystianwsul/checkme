package com.krystianwsul.checkme.gui.utils

import com.krystianwsul.checkme.gui.base.SnackbarListener

fun interface SnackbarData {

    fun show(snackbarListener: SnackbarListener)
}