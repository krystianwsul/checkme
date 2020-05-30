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

    override fun onCreateDialog(savedInstanceState: Bundle?) = MaterialAlertDialogBuilder(requireContext()).setMessage(R.string.editAllReminders)
            .setNegativeButton(R.string.allReminders) { _, _ -> listener(false) }
            .setPositiveButton(R.string.justThisOne) { _, _ -> listener(true) }
            .setNeutralButton(R.string.removeInstancesCancel) { _, _ -> }
            .create()
}
