package com.krystianwsul.checkme.gui.utils

import com.krystianwsul.checkme.gui.base.AbstractActivity
import com.krystianwsul.checkme.gui.dialogs.TwoChoicesCancelDialogFragment
import com.krystianwsul.checkme.gui.edit.EditActivity
import com.krystianwsul.checkme.gui.edit.EditParameters
import com.krystianwsul.checkme.utils.tryGetFragment
import com.krystianwsul.common.utils.InstanceKey

class CopyAllRemindersDelegate(private val activity: AbstractActivity) {

    companion object {

        private const val TAG = "copyAllReminders"
    }

    private val listener = { positive: Boolean, instanceKey: InstanceKey ->
        val copySource = if (positive)
            EditParameters.Copy.CopySource.Task(instanceKey.taskKey)
        else
            EditParameters.Copy.CopySource.Instance(instanceKey)

        activity.startActivity(EditActivity.getParametersIntent(EditParameters.Copy(copySource)))
    }

    init {
        activity.tryGetFragment<TwoChoicesCancelDialogFragment<InstanceKey>>(TAG)
            ?.listener = listener
    }

    fun showDialog(instanceKey: InstanceKey) {
        TwoChoicesCancelDialogFragment.newInstance(TwoChoicesCancelDialogFragment.Parameters.copyAllReminders(instanceKey))
            .also { it.listener = listener }
            .show(activity.supportFragmentManager, TAG)
    }
}