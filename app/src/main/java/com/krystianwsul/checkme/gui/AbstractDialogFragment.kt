package com.krystianwsul.checkme.gui

import androidx.appcompat.app.AppCompatDialogFragment
import com.krystianwsul.checkme.MyCrashlytics

abstract class AbstractDialogFragment : AppCompatDialogFragment() {

    override fun onResume() {
        MyCrashlytics.log(javaClass.simpleName + ".onResume")

        super.onResume()
    }
}
