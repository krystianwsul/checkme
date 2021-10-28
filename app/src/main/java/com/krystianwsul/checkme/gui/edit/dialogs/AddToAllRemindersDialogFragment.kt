package com.krystianwsul.checkme.gui.edit.dialogs

import android.os.Bundle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.gui.base.AbstractDialogFragment

class AddToAllRemindersDialogFragment : AbstractDialogFragment() {

    companion object {

        fun newInstance() = AddToAllRemindersDialogFragment()
    }

    lateinit var listener: (Boolean) -> Unit

    override fun onCreateDialog(savedInstanceState: Bundle?) =
        MaterialAlertDialogBuilder(requireContext()).setMessage(R.string.addToAllReminders)
            .setPositiveButton(R.string.toAllReminders) { _, _ -> listener(true) }
            .setNegativeButton(R.string.justToThisReminder) { _, _ -> listener(false) }
            .setNeutralButton(R.string.removeInstancesCancel) { _, _ -> }
            .create()
}
