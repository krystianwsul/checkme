package com.krystianwsul.checkme.gui.utils

import com.krystianwsul.checkme.gui.base.AbstractActivity
import com.krystianwsul.checkme.gui.dialogs.TwoChoicesCancelDialogFragment
import com.krystianwsul.checkme.gui.edit.EditActivity
import com.krystianwsul.checkme.gui.edit.EditParameters
import com.krystianwsul.checkme.utils.tryGetFragment
import com.krystianwsul.common.utils.TaskKey

class CopyAllRemindersDelegate(private val activity: AbstractActivity) {

    companion object {

        private const val TAG = "copyAllReminders"
    }

    private val listener = { positive: Boolean, taskKey: TaskKey ->
        val intent = EditActivity.getParametersIntent(EditParameters.Copy(taskKey))

        activity.startActivity(intent)
    }

    init {
        activity.tryGetFragment<TwoChoicesCancelDialogFragment<TaskKey>>(TAG)
            ?.listener = listener
    }

    fun showDialog(taskKey: TaskKey) {
        TwoChoicesCancelDialogFragment.newInstance(TwoChoicesCancelDialogFragment.Parameters.copyAllReminders(taskKey))
            .also { it.listener = listener }
            .show(activity.supportFragmentManager, TAG)
    }
}