package com.krystianwsul.checkme.gui.utils

import com.krystianwsul.checkme.gui.base.AbstractActivity
import com.krystianwsul.checkme.gui.dialogs.TwoChoicesCancelDialogFragment
import com.krystianwsul.checkme.gui.edit.EditActivity
import com.krystianwsul.checkme.gui.edit.EditParameters
import com.krystianwsul.checkme.gui.instances.list.GroupListViewModel
import com.krystianwsul.checkme.utils.tryGetFragment
import com.krystianwsul.common.utils.InstanceKey
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo

class CopyAllRemindersDelegate(
    private val activity: AbstractActivity,
    groupListViewModel: GroupListViewModel,
    compositeDisposable: CompositeDisposable,
) {

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

        groupListViewModel.copyInstanceKeyRelay
            .subscribe {
                if (it.showDialog) {
                    showDialog(it.instanceKey)
                } else {
                    activity.startActivity(
                        EditActivity.getParametersIntent(
                            EditParameters.Copy(EditParameters.Copy.CopySource.Instance(it.instanceKey))
                        )
                    )
                }
            }
            .addTo(compositeDisposable)
    }

    fun showDialog(instanceKey: InstanceKey) {
        TwoChoicesCancelDialogFragment.newInstance(TwoChoicesCancelDialogFragment.Parameters.copyAllReminders(instanceKey))
            .also { it.listener = listener }
            .show(activity.supportFragmentManager, TAG)
    }
}