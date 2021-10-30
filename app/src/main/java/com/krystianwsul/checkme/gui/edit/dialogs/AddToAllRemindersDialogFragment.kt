package com.krystianwsul.checkme.gui.edit.dialogs

import android.os.Bundle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.gui.base.AbstractDialogFragment
import kotlin.properties.Delegates.notNull

class AddToAllRemindersDialogFragment : AbstractDialogFragment() {

    companion object {

        private val KEY_AND_OPEN = "andOpen"

        fun newInstance(andOpen: Boolean) = AddToAllRemindersDialogFragment().apply {
            arguments = Bundle().apply { putBoolean(KEY_AND_OPEN, andOpen) }
        }
    }

    lateinit var listener: (allReminders: Boolean, andOpen: Boolean) -> Unit

    private var andOpen by notNull<Boolean>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        andOpen = requireArguments().getBoolean(KEY_AND_OPEN)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?) =
        MaterialAlertDialogBuilder(requireContext()).setMessage(R.string.addToAllReminders)
            .setPositiveButton(R.string.toAllReminders) { _, _ -> listener(true, andOpen) }
            .setNegativeButton(R.string.justToThisReminder) { _, _ -> listener(false, andOpen) }
            .setNeutralButton(R.string.removeInstancesCancel) { _, _ -> }
            .create()
}
