package com.krystianwsul.checkme.gui.base

import androidx.appcompat.app.AppCompatDialogFragment
import com.krystianwsul.checkme.MyCrashlytics
import io.reactivex.rxjava3.disposables.CompositeDisposable

abstract class AbstractDialogFragment : AppCompatDialogFragment() {

    protected val viewCreatedDisposable = CompositeDisposable()
    protected val startDisposable = CompositeDisposable()

    override fun onResume() {
        MyCrashlytics.logMethod(this)

        super.onResume()
    }

    override fun onStop() {
        startDisposable.clear()

        super.onStop()
    }

    override fun onDestroyView() {
        viewCreatedDisposable.clear()

        super.onDestroyView()
    }
}
