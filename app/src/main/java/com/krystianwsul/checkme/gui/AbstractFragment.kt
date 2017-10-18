package com.krystianwsul.checkme.gui

import android.content.Context
import android.support.v4.app.Fragment

import com.krystianwsul.checkme.MyCrashlytics

abstract class AbstractFragment : Fragment() {

    override fun onAttach(context: Context) {
        MyCrashlytics.log(javaClass.simpleName + ".onAttach")
        super.onAttach(context)
    }

    override fun onResume() {
        MyCrashlytics.log(javaClass.simpleName + ".onResume")
        super.onResume()
    }

    override fun onDetach() {
        MyCrashlytics.log(javaClass.simpleName + ".onDetach")
        super.onDetach()
    }
}
