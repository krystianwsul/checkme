package com.krystianwsul.checkme.gui

import android.os.Bundle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.utils.TaskKey

class RemoveInstancesDialogFragment : AbstractDialogFragment() {

    companion object {

        private const val KEY_TASKS = "tasks"

        fun newInstance(taskKeys: Set<TaskKey>) = RemoveInstancesDialogFragment().apply {
            // todo check if eligible instances exist
            arguments = Bundle().apply {
                putParcelableArrayList(KEY_TASKS, ArrayList(taskKeys))
            }
        }
    }

    lateinit var taskKeys: Set<TaskKey>

    lateinit var listener: (Set<TaskKey>, Boolean) -> Unit

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        taskKeys = arguments!!.getParcelableArrayList<TaskKey>(KEY_TASKS)!!.toSet()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?) = MaterialAlertDialogBuilder(requireContext()).setMessage(R.string.removeInstancesMessage)
            .setNegativeButton(R.string.removeInstancesNo) { _, _ -> listener(taskKeys, false) }
            .setPositiveButton(R.string.removeInstancesYes) { _, _ -> listener(taskKeys, true) }
            .create()
}
