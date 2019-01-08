package com.krystianwsul.checkme.gui

import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.krystianwsul.checkme.R

interface SnackbarListener {

    companion object {

        val DURATION = 5000

        var deleting = false
            private set
    }

    val snackbarParent: CoordinatorLayout

    fun showSnackbar(count: Int, action: () -> Unit) {
        check(!deleting)

        Snackbar.make(snackbarParent, snackbarParent.context.getString(R.string.snackbar, count.toString()), DURATION).apply {
            setAction(R.string.undo) { action() }

            deleting = true

            addCallback(object : BaseTransientBottomBar.BaseCallback<Snackbar>() {

                override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                    check(deleting)

                    deleting = false
                }
            })

            show()
        }
    }
}