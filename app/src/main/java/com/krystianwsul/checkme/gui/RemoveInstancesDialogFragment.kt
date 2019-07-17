package com.krystianwsul.checkme.gui

import android.os.Bundle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.krystianwsul.checkme.R

class RemoveInstancesDialogFragment : AbstractDialogFragment() {

    companion object {

        private const val KEY_MULTIPLE_INSTANCES = "multipleInstances"

        fun newInstance(multipleInstances: Boolean) = RemoveInstancesDialogFragment().apply {
            arguments = Bundle().apply { putBoolean(KEY_MULTIPLE_INSTANCES, multipleInstances) }
        }
    }

    private var negativeId: Int = -1
    private var positiveId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (arguments!!.getBoolean(KEY_MULTIPLE_INSTANCES)) {
            negativeId = R.string.removeInstancesNoPlural
            positiveId = R.string.removeInstancesYesPlural
        } else {
            negativeId = R.string.removeInstancesNoSingular
            positiveId = R.string.removeInstancesYesSingular
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?) = MaterialAlertDialogBuilder(requireContext()).setMessage(R.string.removeInstancesMessage)
            .setNegativeButton(negativeId) { _, _ -> }
            .setPositiveButton(positiveId) { _, _ -> }
            .create()
}
