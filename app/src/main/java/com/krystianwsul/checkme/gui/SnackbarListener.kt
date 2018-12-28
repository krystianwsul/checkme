package com.krystianwsul.checkme.gui

import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.snackbar.Snackbar
import com.krystianwsul.checkme.R

interface SnackbarListener {

    val snackbarParent: CoordinatorLayout

    fun showSnackbar(count: Int, action: () -> Unit) {
        Snackbar.make(snackbarParent, snackbarParent.context.getString(R.string.snackbar, count.toString()), Snackbar.LENGTH_LONG).apply {
            setAction(R.string.undo) { action() }

            show()
        }
    }
}