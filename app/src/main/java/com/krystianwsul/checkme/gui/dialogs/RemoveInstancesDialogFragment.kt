package com.krystianwsul.checkme.gui.dialogs

import android.os.Bundle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.gui.base.AbstractDialogFragment
import com.krystianwsul.common.utils.TaskKey
import java.io.Serializable

class RemoveInstancesDialogFragment : AbstractDialogFragment() {

    companion object {

        private const val KEY_PAYLOAD = "payload"

        fun newInstance(taskKeys: Set<TaskKey>) = newInstance(taskKeys.toHashSet() as Serializable)

        fun newInstance(payload: Serializable) = RemoveInstancesDialogFragment().apply {
            arguments = Bundle().apply {
                putSerializable(KEY_PAYLOAD, payload)
            }
        }
    }

    private lateinit var payload: Serializable

    lateinit var listener: (Serializable, Boolean) -> Unit

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        payload = requireArguments().getSerializable(KEY_PAYLOAD)!!
    }

    override fun onCreateDialog(savedInstanceState: Bundle?) = MaterialAlertDialogBuilder(requireContext()).setMessage(R.string.removeInstancesMessage)
            .setNegativeButton(R.string.removeInstancesNo) { _, _ -> listener(payload, false) }
            .setPositiveButton(R.string.removeInstancesYes) { _, _ -> listener(payload, true) }
            .setNeutralButton(R.string.removeInstancesCancel) { _, _ -> }
            .create()
}
