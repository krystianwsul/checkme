package com.krystianwsul.checkme.gui.instances

import android.os.Bundle
import android.os.Parcelable
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.gui.base.AbstractDialogFragment
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.TimePair
import com.krystianwsul.common.utils.Parcelize
import com.krystianwsul.common.utils.TaskKey

class SubtaskDialogFragment : AbstractDialogFragment() {

    companion object {

        private const val KEY_RESULT_DATA = "resultData"

        fun newInstance(resultData: ResultData) = SubtaskDialogFragment().apply {
            arguments = Bundle().apply { putParcelable(KEY_RESULT_DATA, resultData) }
        }
    }

    private lateinit var resultData: ResultData

    lateinit var listener: (Result) -> Unit

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        resultData = requireArguments().getParcelable(KEY_RESULT_DATA)!!
    }

    override fun onCreateDialog(savedInstanceState: Bundle?) = MaterialAlertDialogBuilder(requireContext()).setMessage(R.string.subtaskDialogMessage)
            .setPositiveButton(R.string.add_task_this_time) { _, _ -> listener(Result.SameTime(resultData)) }
            .setNegativeButton(R.string.addTaskList) { _, _ -> listener(Result.Subtask(resultData)) }
            .setNeutralButton(R.string.removeInstancesCancel) { _, _ -> }
            .create()

    sealed class Result {

        abstract val resultData: ResultData

        data class SameTime(override val resultData: ResultData) : Result()

        data class Subtask(override val resultData: ResultData) : Result()
    }

    @Parcelize
    data class ResultData(
        val taskKey: TaskKey,
        val instanceDate: Date,
        val createTaskTimePair: TimePair,
    ) : Parcelable
}
