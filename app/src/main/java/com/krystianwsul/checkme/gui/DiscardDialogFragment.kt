package com.krystianwsul.checkme.gui

import android.os.Bundle
import com.afollestad.materialdialogs.MaterialDialog
import com.krystianwsul.checkme.R

class DiscardDialogFragment : AbstractDialogFragment() {

    companion object {

        fun newInstance() = DiscardDialogFragment()
    }

    lateinit var discardDialogListener: () -> Unit

    override fun onCreateDialog(savedInstanceState: Bundle?) = MaterialDialog.Builder(requireActivity())
            .content(R.string.discard_changes)
            .negativeText(android.R.string.cancel)
            .positiveText(R.string.discard)
            .onPositive { _, _ -> discardDialogListener() }
            .show()!!
}
