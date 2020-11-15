package com.krystianwsul.checkme.gui.base

import android.view.View
import androidx.annotation.StringRes
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.isVisible
import com.google.android.material.snackbar.Snackbar
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.R

interface SnackbarListener {

    val snackbarParent: CoordinatorLayout

    val anchor
        get() = snackbarParent.run {
            findViewById<View>(R.id.bottomFab)!!.takeIf { it.isVisible } ?: findViewById(R.id.bottomAppBar)!!
        }

    /*
    SHORT = 1500
    LONG = 2750
     */

    fun showSnackbarRemoved(count: Int, action: () -> Unit) = showSnackbar(R.string.snackbarRemoved, count, Snackbar.LENGTH_LONG, action)

    fun showSnackbarDone(count: Int, action: () -> Unit) = showSnackbar(R.string.snackbarDone, count, Snackbar.LENGTH_SHORT, action)

    fun showSnackbarNotDone(count: Int, action: () -> Unit) = showSnackbar(R.string.snackbarNotDone, count, Snackbar.LENGTH_SHORT, action)

    fun showInstanceMarkedDone() = showSnackbar(snackbarParent.context.getString(R.string.instanceMarkedDone), Snackbar.LENGTH_LONG)

    fun showSnackbarHour(count: Int, action: () -> Unit) = showSnackbar(R.string.snackbarHour, count, Snackbar.LENGTH_SHORT, action)

    fun showText(message: String, duration: Int) = showSnackbar(message, duration)

    private fun showSnackbar(@StringRes messageId: Int, count: Int, duration: Int, action: () -> Unit) = showSnackbar(snackbarParent.context.getString(messageId, count.toString()), duration, action)

    private fun showSnackbar(message: String, duration: Int, action: (() -> Unit)? = null) {
        MyCrashlytics.logMethod(this)

        Snackbar.make(snackbarParent, message, duration).apply {
            action?.let {
                setAction(R.string.undo) { action() }
            }

            anchorView = anchor

            show()
        }
    }
}