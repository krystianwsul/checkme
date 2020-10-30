package com.krystianwsul.checkme.gui.edit.dialogs

import android.os.Bundle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.gui.base.AbstractDialogFragment
import kotlin.properties.Delegates.notNull

class AllRemindersDialogFragment : AbstractDialogFragment() {

    companion object {

        private const val KEY_PLURAL = "plural"

        fun newInstance(plural: Boolean) = AllRemindersDialogFragment().apply {
            arguments = Bundle().apply { putBoolean(KEY_PLURAL, plural) }
        }
    }

    lateinit var listener: (Boolean) -> Unit

    private var plural by notNull<Boolean>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        plural = requireArguments().getBoolean(KEY_PLURAL)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?) = MaterialAlertDialogBuilder(requireContext()).setMessage(R.string.joinAllFutureReminders)
            .setPositiveButton(R.string.allFutureReminders) { _, _ -> listener(true) }
            .setNegativeButton(if (plural) R.string.justTheseReminders else R.string.justThisReminder) { _, _ ->
                listener(false)
            }
            .setNeutralButton(R.string.removeInstancesCancel) { _, _ -> }
            .create()
}
