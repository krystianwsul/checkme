package com.krystianwsul.checkme.gui

import android.view.View
import androidx.annotation.StringRes
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.R

interface SnackbarListener {

    companion object {

        private val hashes = mutableSetOf<Int>()

        val deleting get() = hashes.size > 0
    }

    val snackbarParent: CoordinatorLayout

    val anchor get() = snackbarParent.findViewById<View>(R.id.bottomAnchor)

    /*
    SHORT = 1500
    LONG = 2750
     */

    fun showSnackbarRemoved(count: Int, action: () -> Unit) = showSnackbar(R.string.snackbarRemoved, count, Snackbar.LENGTH_LONG, action)

    fun showSnackbarDone(count: Int, action: () -> Unit) = showSnackbar(R.string.snackbarDone, count, Snackbar.LENGTH_SHORT, action)

    fun showSnackbarNotDone(count: Int, action: () -> Unit) = showSnackbar(R.string.snackbarNotDone, count, Snackbar.LENGTH_SHORT, action)

    fun showInstanceMarkedDone() = showSnackbar(snackbarParent.context.getString(R.string.instanceMarkedDone), Snackbar.LENGTH_LONG)

    fun showText(message: String, duration: Int) = showSnackbar(message, duration)

    private fun showSnackbar(@StringRes messageId: Int, count: Int, duration: Int, action: () -> Unit) = showSnackbar(snackbarParent.context.getString(messageId, count.toString()), duration, true, action)

    private fun showSnackbar(message: String, duration: Int, preventIrrelevant: Boolean = false, action: (() -> Unit)? = null) {
        MyCrashlytics.logMethod(this)

        Snackbar.make(snackbarParent, message, duration).apply {
            val hash = hashCode()
            if (preventIrrelevant)
                hashes.add(hash)

            action?.let {
                setAction(R.string.undo) { action() }
            }

            addCallback(object : BaseTransientBottomBar.BaseCallback<Snackbar>() {

                override fun onShown(transientBottomBar: Snackbar) = Unit

                override fun onDismissed(transientBottomBar: Snackbar, event: Int) {
                    hashes.remove(hash)
                }
            })

            anchorView = anchor

            show()
        }
    }
}