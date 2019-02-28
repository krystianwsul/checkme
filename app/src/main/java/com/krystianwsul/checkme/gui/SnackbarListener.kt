package com.krystianwsul.checkme.gui

import androidx.annotation.StringRes
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

    fun showSnackbarRemoved(count: Int, action: () -> Unit) = showSnackbar(R.string.snackbarRemoved, count, action)

    fun showSnackbarDone(count: Int, action: () -> Unit) = showSnackbar(R.string.snackbarDone, count, action)

    fun showSnackbarNotDone(count: Int, action: () -> Unit) = showSnackbar(R.string.snackbarNotDone, count, action)

    private fun showSnackbar(@StringRes messageId: Int, count: Int, action: () -> Unit) = showSnackbar(snackbarParent.context.getString(messageId, count.toString()), action)

    private fun showSnackbar(message: String, action: () -> Unit) {
        MyCrashlytics.logMethod(this)

        check(++SnackbarListener.count > 0)

        Snackbar.make(snackbarParent, message, 5000).apply {
            setAction(R.string.undo) { action() }

            addCallback(object : BaseTransientBottomBar.BaseCallback<Snackbar>() {

                override fun onShown(transientBottomBar: Snackbar) = MyCrashlytics.logMethod(this@apply)

                override fun onDismissed(transientBottomBar: Snackbar, event: Int) {
                    MyCrashlytics.logMethod(this@apply)

                    check(SnackbarListener.count-- > 0)
                }
            })

            anchorView = snackbarParent.findViewById(R.id.bottomAnchor)

            show()
        }
    }
}