package com.krystianwsul.checkme.gui

interface SnackbarListener {

    fun showSnackbar(count: Int, action: () -> Unit)
}