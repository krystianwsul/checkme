package com.krystianwsul.checkme.gui

import android.content.Context
import android.support.v4.app.Fragment

import com.krystianwsul.checkme.MyCrashlytics

abstract class AbstractFragment : Fragment() {

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

    override fun onDetach() {
        MyCrashlytics.log(javaClass.simpleName + ".onDetach " + hashCode())
        super.onDetach()
    }
}
