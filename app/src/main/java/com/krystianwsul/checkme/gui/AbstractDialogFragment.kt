package com.krystianwsul.checkme.gui

import android.support.v7.app.AppCompatDialogFragment
import com.krystianwsul.checkme.MyCrashlytics

abstract class AbstractDialogFragment : AppCompatDialogFragment() {

    override fun onResume() {
        MyCrashlytics.log(javaClass.simpleName + ".onResume")

        super.onResume()
    }
}
