package com.krystianwsul.checkme.gui.edit.dialogs

import android.os.Bundle
import android.os.Parcelable
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.gui.base.AbstractDialogFragment
import kotlinx.parcelize.Parcelize

class AddToAllRemindersDialogFragment<T : Parcelable> : AbstractDialogFragment() {

    companion object {

        private const val KEY_PARAMETERS = "parameters"

        fun <T : Parcelable> newInstance(parameters: Parameters<T>) = AddToAllRemindersDialogFragment<T>().apply {
            arguments = Bundle().apply { putParcelable(KEY_PARAMETERS, parameters) }
        }
    }

    lateinit var listener: (allReminders: Boolean, payload: T) -> Unit

    private lateinit var payload: T

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val parameters = requireArguments().getParcelable<Parameters<T>>(KEY_PARAMETERS)!!

        payload = parameters.payload
    }

    override fun onCreateDialog(savedInstanceState: Bundle?) =
        MaterialAlertDialogBuilder(requireContext()).setMessage(R.string.addToAllReminders)
            .setPositiveButton(R.string.toAllReminders) { _, _ -> listener(true, payload) }
            .setNegativeButton(R.string.justToThisReminder) { _, _ -> listener(false, payload) }
            .setNeutralButton(R.string.removeInstancesCancel) { _, _ -> }
            .create()

    @Parcelize
    data class Parameters<T : Parcelable>(val payload: T) : Parcelable {

        companion object {

            fun newAddToAllReminders(andOpen: Boolean) = Parameters(BooleanPayload(andOpen))
        }

        @Parcelize
        data class BooleanPayload(val value: Boolean) : Parcelable
    }
}
