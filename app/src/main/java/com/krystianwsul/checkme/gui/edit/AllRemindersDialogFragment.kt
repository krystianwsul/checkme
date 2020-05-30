package com.krystianwsul.checkme.gui.edit

import android.os.Bundle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.gui.AbstractDialogFragment

class AllRemindersDialogFragment : AbstractDialogFragment() {

    companion object {

        fun newInstance() = AllRemindersDialogFragment()
    }

    lateinit var listener: (Boolean) -> Unit

    override fun onCreateDialog(savedInstanceState: Bundle?) = MaterialAlertDialogBuilder(requireContext()).setMessage(R.string.joinAllFutureReminders)
            .setNegativeButton(R.string.allFutureReminders) { _, _ -> listener(true) }
            .setPositiveButton(R.string.justThese) { _, _ -> listener(false) }
            .setNeutralButton(R.string.removeInstancesCancel) { _, _ -> }
            .create()
}
