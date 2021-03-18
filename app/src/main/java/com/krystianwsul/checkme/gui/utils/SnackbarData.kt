package com.krystianwsul.checkme.gui.utils

import androidx.annotation.CheckResult
import com.krystianwsul.checkme.gui.base.SnackbarListener
import io.reactivex.rxjava3.core.Completable

fun interface SnackbarData {

    @CheckResult
    fun show(snackbarListener: SnackbarListener): Completable
}