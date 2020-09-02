package com.krystianwsul.checkme.gui

import android.os.Bundle
import android.os.Parcelable
import androidx.annotation.StringRes
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.krystianwsul.checkme.R
import kotlinx.android.parcel.Parcelize

class ConfirmDialogFragment : AbstractDialogFragment() {

    companion object {

        private const val KEY_PARAMETERS = "parameters"

        fun newInstance(parameters: Parameters) = ConfirmDialogFragment().apply {
            arguments = Bundle().apply { putParcelable(KEY_PARAMETERS, parameters) }
        }
    }

    lateinit var listener: () -> Unit

    private lateinit var parameters: Parameters

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        parameters = requireArguments().getParcelable(KEY_PARAMETERS)!!
    }

    override fun onCreateDialog(savedInstanceState: Bundle?) = MaterialAlertDialogBuilder(requireContext()).setMessage(parameters.message)
            .setNegativeButton(android.R.string.cancel) { _, _ -> }
            .setPositiveButton(parameters.positive) { _, _ -> listener() }
            .create()

    @Parcelize
    data class Parameters(
            @StringRes val message: Int,
            @StringRes val positive: Int
    ) : Parcelable {

        companion object {

            val Discard = Parameters(R.string.discard_changes, R.string.discard)
        }
    }
}
