package com.krystianwsul.checkme.gui.edit.dialogs

import android.os.Bundle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.gui.base.AbstractDialogFragment

class AllRemindersDialogFragment : AbstractDialogFragment() {

    companion object {

        fun newInstance() = AllRemindersDialogFragment()
    }

    lateinit var listener: (Boolean) -> Unit

    override fun onCreateDialog(savedInstanceState: Bundle?) =
        MaterialAlertDialogBuilder(requireContext()).setMessage(R.string.joinAllFutureReminders)
            .setPositiveButton(R.string.allFutureReminders) { _, _ -> listener(true) }
            .setNegativeButton(R.string.justTheseReminders) { _, _ ->
                listener(false)
            }
            .setNeutralButton(R.string.removeInstancesCancel) { _, _ -> }
            .create()
}
