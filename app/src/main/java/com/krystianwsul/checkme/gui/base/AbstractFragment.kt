package com.krystianwsul.checkme.gui.base

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment

import com.krystianwsul.checkme.MyCrashlytics
import io.reactivex.rxjava3.disposables.CompositeDisposable

abstract class AbstractFragment : Fragment() {

    protected val createDisposable = CompositeDisposable()
    protected val viewCreatedDisposable = CompositeDisposable()

    override fun onAttach(context: Context) {
        MyCrashlytics.logMethod(this)
        super.onAttach(context)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        MyCrashlytics.logMethod(this)
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onResume() {
        MyCrashlytics.logMethod(this)
        super.onResume()
    }

    override fun onPause() {
        MyCrashlytics.logMethod(this)
        super.onPause()
    }

    override fun onDestroyView() {
        MyCrashlytics.logMethod(this)

        viewCreatedDisposable.clear()

        super.onDestroyView()
    }

    override fun onDestroy() {
        MyCrashlytics.logMethod(this)

        createDisposable.dispose()

        super.onDestroy()
    }

    override fun onDetach() {
        MyCrashlytics.logMethod(this)
        super.onDetach()
    }
}
