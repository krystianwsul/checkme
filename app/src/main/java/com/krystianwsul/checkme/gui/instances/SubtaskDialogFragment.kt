package com.krystianwsul.checkme.gui.instances

import android.os.Bundle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.gui.base.AbstractDialogFragment
import com.krystianwsul.common.utils.InstanceKey

class SubtaskDialogFragment : AbstractDialogFragment() {

    companion object {

        private const val KEY_INSTANCE_KEY = "instanceKey"

        fun newInstance(instanceKey: InstanceKey) = SubtaskDialogFragment().apply {
            arguments = Bundle().apply { putParcelable(KEY_INSTANCE_KEY, instanceKey) }
        }
    }

    private lateinit var instanceKey: InstanceKey

    lateinit var listener: (Result) -> Unit

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        instanceKey = requireArguments().getParcelable(KEY_INSTANCE_KEY)!!
    }

    override fun onCreateDialog(savedInstanceState: Bundle?) = MaterialAlertDialogBuilder(requireContext()).setMessage(R.string.joinAllFutureReminders)
            .setPositiveButton(R.string.add_task_this_time) { _, _ -> listener(Result.SAME_TIME) }
            .setNegativeButton(R.string.addTaskList) { _, _ -> listener(Result.SUBTASK) }
            .setNeutralButton(R.string.removeInstancesCancel) { _, _ -> }
            .create()

    enum class Result {

        SAME_TIME, SUBTASK
    }
}
