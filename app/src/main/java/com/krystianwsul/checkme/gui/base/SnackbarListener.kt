package com.krystianwsul.checkme.gui.base

import android.view.View
import androidx.annotation.StringRes
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.isVisible
import com.google.android.material.snackbar.Snackbar
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.R
import io.reactivex.rxjava3.core.Maybe

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

    fun showSnackbarRemovedMaybe(count: Int) = showSnackbarMaybe(R.string.snackbarRemoved, count, Snackbar.LENGTH_LONG)

    fun showSnackbarDone(count: Int, action: () -> Unit) = showSnackbarDoneMaybe(count).subscribe { action() }!!
    fun showSnackbarDoneMaybe(count: Int) = showSnackbarMaybe(R.string.snackbarDone, count, Snackbar.LENGTH_SHORT)

    fun showSnackbarNotDoneMaybe(count: Int) = showSnackbarMaybe(R.string.snackbarNotDone, count, Snackbar.LENGTH_SHORT)

    fun showInstanceMarkedDone() = showSnackbar(snackbarParent.context.getString(R.string.instanceMarkedDone), Snackbar.LENGTH_LONG)

    fun showSnackbarHourMaybe(count: Int) = showSnackbarMaybe(R.string.snackbarHour, count, Snackbar.LENGTH_SHORT)

    fun showText(message: String, duration: Int) = showSnackbar(message, duration)

    private fun showSnackbarMaybe(@StringRes messageId: Int, count: Int, duration: Int) =
            Maybe.create<Unit> { emitter ->
                showSnackbar(
                        messageId,
                        count,
                        duration,
                        { emitter.onSuccess(Unit) },
                        { emitter.onComplete() }
                )
            }!!

    private fun showSnackbar(
            @StringRes messageId: Int,
            count: Int,
            duration: Int,
            action: () -> Unit,
            onDismiss: (() -> Unit)? = null,
    ) = showSnackbar(snackbarParent.context.getString(messageId, count.toString()), duration, action, onDismiss)

    private fun showSnackbar(
            message: String,
            duration: Int,
            action: (() -> Unit)? = null,
            onDismiss: (() -> Unit)? = null,
    ) {
        MyCrashlytics.logMethod(this)

        Snackbar.make(snackbarParent, message, duration).apply {
            action?.let {
                setAction(R.string.undo) { action() }
            }

            onDismiss?.let {
                addCallback(object : Snackbar.Callback() {

                    override fun onDismissed(transientBottomBar: Snackbar?, event: Int) = it()
                })
            }

            anchorView = anchor

            show()
        }
    }
}