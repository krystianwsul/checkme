package com.krystianwsul.checkme.gui

import android.content.Context
import android.support.v4.app.Fragment

import com.krystianwsul.checkme.MyCrashlytics
import io.reactivex.disposables.CompositeDisposable

abstract class AbstractFragment : Fragment() {

    protected val createDisposable = CompositeDisposable()
    protected val viewCreatedDisposable = CompositeDisposable()

    override fun onAttach(context: Context) {
        MyCrashlytics.log(javaClass.simpleName + ".onAttach " + hashCode())
        super.onAttach(context)
    }

    override fun onResume() {
        MyCrashlytics.log(javaClass.simpleName + ".onResume " + hashCode())
        super.onResume()
    }

    override fun onPause() {
        MyCrashlytics.log(javaClass.simpleName + ".onPause " + hashCode())
        super.onPause()
    }

    override fun onDestroyView() {
        MyCrashlytics.log(javaClass.simpleName + ".onDestroyView " + hashCode())

        viewCreatedDisposable.clear()

        super.onDestroyView()
    }

    override fun onDestroy() {
        MyCrashlytics.log(javaClass.simpleName + ".onDestroy " + hashCode())

        createDisposable.dispose()

        super.onDestroy()
    }

    override fun onDetach() {
        MyCrashlytics.log(javaClass.simpleName + ".onDetach " + hashCode())
        super.onDetach()
    }
}
