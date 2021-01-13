package com.krystianwsul.checkme.gui.dialogs

import android.os.Bundle
import android.os.Parcelable
import androidx.annotation.StringRes
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.gui.base.AbstractDialogFragment
import kotlinx.parcelize.Parcelize

class ConfirmDialogFragment : AbstractDialogFragment() {

    companion object {

        private const val KEY_PARAMETERS = "parameters"

        fun newInstance(parameters: Parameters) = ConfirmDialogFragment().apply {
            arguments = Bundle().apply { putParcelable(KEY_PARAMETERS, parameters) }
        }
    }

    lateinit var listener: (payload: Parcelable?) -> Unit

    private lateinit var parameters: Parameters

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        parameters = requireArguments().getParcelable(KEY_PARAMETERS)!!
    }

    override fun onCreateDialog(savedInstanceState: Bundle?) =
            MaterialAlertDialogBuilder(requireContext()).apply { parameters.title?.let(::setTitle) }
                    .setMessage(parameters.message)
                    .setNegativeButton(android.R.string.cancel) { _, _ -> }
                    .setPositiveButton(parameters.positive) { _, _ -> listener(parameters.payload) }
                    .create()

    @Parcelize
    data class Parameters(
            @StringRes val message: Int,
            @StringRes val positive: Int,
            @StringRes val title: Int? = null,
            val payload: Parcelable? = null,
    ) : Parcelable {

        companion object {

            val Discard = Parameters(R.string.discard_changes, R.string.discard)
        }
    }
}
