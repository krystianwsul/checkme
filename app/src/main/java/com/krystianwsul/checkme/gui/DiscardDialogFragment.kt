package com.krystianwsul.checkme.gui

import android.os.Bundle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.krystianwsul.checkme.R

class DiscardDialogFragment : AbstractDialogFragment() {

    companion object {

        fun newInstance() = DiscardDialogFragment()
    }

    lateinit var discardDialogListener: () -> Unit

    override fun onCreateDialog(savedInstanceState: Bundle?) = MaterialAlertDialogBuilder(requireContext()).setMessage(R.string.discard_changes)
            .setNegativeButton(android.R.string.cancel) { _, _ -> }
            .setPositiveButton(R.string.discard) { _, _ -> discardDialogListener() }
            .create()
}
