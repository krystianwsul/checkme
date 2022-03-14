package com.krystianwsul.checkme.gui.edit.dialogs

import android.os.Bundle
import android.os.Parcelable
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.gui.base.AbstractDialogFragment
import kotlinx.parcelize.Parcelize
import kotlin.properties.Delegates.notNull

class AddToAllRemindersDialogFragment : AbstractDialogFragment() {

    companion object {

        private const val KEY_PARAMETERS = "parameters"

        fun newInstance(parameters: Parameters) = AddToAllRemindersDialogFragment().apply {
            arguments = Bundle().apply { putParcelable(KEY_PARAMETERS, parameters) }
        }
    }

    lateinit var listener: (allReminders: Boolean, andOpen: Boolean) -> Unit

    private var andOpen by notNull<Boolean>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val parameters = requireArguments().getParcelable<Parameters>(KEY_PARAMETERS)!!

        andOpen = parameters.andOpen
    }

    override fun onCreateDialog(savedInstanceState: Bundle?) =
        MaterialAlertDialogBuilder(requireContext()).setMessage(R.string.addToAllReminders)
            .setPositiveButton(R.string.toAllReminders) { _, _ -> listener(true, andOpen) }
            .setNegativeButton(R.string.justToThisReminder) { _, _ -> listener(false, andOpen) }
            .setNeutralButton(R.string.removeInstancesCancel) { _, _ -> }
            .create()

    @Parcelize
    data class Parameters(val andOpen: Boolean) : Parcelable
}
