package com.krystianwsul.checkme.gui

import android.support.v4.app.DialogFragment

import com.krystianwsul.checkme.MyCrashlytics

abstract class AbstractDialogFragment : DialogFragment() {

    override fun onResume() {
        MyCrashlytics.log(javaClass.simpleName + ".onResume")

        super.onResume()
    }
}
