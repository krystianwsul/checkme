package com.krystianwsul.checkme.gui.dialogs

import android.os.Bundle
import android.os.Parcelable
import androidx.annotation.StringRes
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.gui.base.AbstractDialogFragment
import com.krystianwsul.common.utils.InstanceKey
import kotlinx.parcelize.Parcelize

class TwoChoicesCancelDialogFragment<T : Parcelable> : AbstractDialogFragment() {

    companion object {

        private const val KEY_PARAMETERS = "parameters"

        fun <T : Parcelable> newInstance(parameters: Parameters<T>) = TwoChoicesCancelDialogFragment<T>().apply {
            arguments = Bundle().apply { putParcelable(KEY_PARAMETERS, parameters) }
        }
    }

    lateinit var listener: (positive: Boolean, payload: T) -> Unit

    private lateinit var parameters: Parameters<T>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        parameters = requireArguments().getParcelable(KEY_PARAMETERS)!!
    }

    override fun onCreateDialog(savedInstanceState: Bundle?) =
        MaterialAlertDialogBuilder(requireContext()).setMessage(parameters.message)
            .setPositiveButton(parameters.positive) { _, _ -> listener(true, parameters.payload) } // top/right
            .setNegativeButton(parameters.negative) { _, _ -> listener(false, parameters.payload) } // middle/middle
            .setNeutralButton(R.string.removeInstancesCancel) { _, _ -> } // bottom/left
            .create()

    @Parcelize
    class Parameters<T : Parcelable>(
        @StringRes val message: Int,
        @StringRes val positive: Int, // top/right
        @StringRes val negative: Int, // middle/middle
        val payload: T,
    ) : Parcelable {

        companion object {

            fun newAddToAllReminders(andOpen: Boolean) = Parameters(
                R.string.addToAllReminders,
                R.string.toAllReminders,
                R.string.justToThisReminder,
                BooleanPayload(andOpen),
            )

            fun copyAllReminders(instanceKey: InstanceKey) = Parameters(
                R.string.copyAllReminders,
                R.string.allReminders,
                R.string.justThisReminder,
                instanceKey,
            )
        }

        @Parcelize
        class BooleanPayload(val value: Boolean) : Parcelable

        @Parcelize
        object UnitParcelable : Parcelable
    }
}
