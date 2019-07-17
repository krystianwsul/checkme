package com.krystianwsul.checkme.gui

import androidx.appcompat.app.AppCompatDialogFragment
import com.krystianwsul.checkme.MyCrashlytics
import io.reactivex.disposables.CompositeDisposable

abstract class AbstractDialogFragment : AppCompatDialogFragment() {

    protected val startDisposable = CompositeDisposable()

    override fun onResume() {
        MyCrashlytics.logMethod(this)

        super.onResume()
    }

    override fun onStop() {
        startDisposable.clear()

        super.onStop()
    }
}
