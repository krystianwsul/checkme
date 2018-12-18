package com.krystianwsul.checkme.gui

import android.os.Bundle
import com.afollestad.materialdialogs.MaterialDialog
import com.krystianwsul.checkme.R

class DiscardDialogFragment : AbstractDialogFragment() {

    companion object {

        fun newInstance() = DiscardDialogFragment()
    }

    lateinit var discardDialogListener: () -> Unit

    override fun onCreateDialog(savedInstanceState: Bundle?) = MaterialDialog(requireActivity()).show {
        message(R.string.discard_changes)
        negativeButton(android.R.string.cancel)
        positiveButton(R.string.discard) { discardDialogListener() }
    }
}
