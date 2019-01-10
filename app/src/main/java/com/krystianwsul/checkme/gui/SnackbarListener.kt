package com.krystianwsul.checkme.gui

import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.R

interface SnackbarListener {

    companion object {

        private var count = 0

        val deleting get() = count > 0
    }

    val snackbarParent: CoordinatorLayout

    fun showSnackbar(count: Int, action: () -> Unit) {
        MyCrashlytics.logMethod(this)

        check(++SnackbarListener.count > 0)

        Snackbar.make(snackbarParent, snackbarParent.context.getString(R.string.snackbar, count.toString()), 5000).apply {
            setAction(R.string.undo) { action() }

            addCallback(object : BaseTransientBottomBar.BaseCallback<Snackbar>() {

                override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                    MyCrashlytics.logMethod(this)

                    check(SnackbarListener.count-- > 0)
                }
            })

            show()
        }
    }
}