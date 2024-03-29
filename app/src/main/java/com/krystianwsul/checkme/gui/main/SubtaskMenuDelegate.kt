package com.krystianwsul.checkme.gui.main

import android.app.Activity
import android.content.Context
import arrow.core.Tuple4
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.gui.edit.EditActivity
import com.krystianwsul.checkme.gui.edit.EditParameters
import com.krystianwsul.checkme.gui.edit.EditParentHint
import com.krystianwsul.checkme.gui.instances.list.GroupListFragment
import com.krystianwsul.checkme.gui.utils.BottomFabMenuDelegate
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.TimePair
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.common.utils.ProjectKey
import kotlinx.parcelize.Parcelize

@Parcelize
class SubtaskMenuDelegate(
    val instanceKey: InstanceKey?,
    private val instanceDate: Date,
    private val createTaskTimePair: TimePair,
    private val projectKey: ProjectKey.Shared?,
    private val showAddToTime: Boolean = true,
) : BottomFabMenuDelegate.MenuDelegate {

    override fun getItems() = listOfNotNull(
        instanceKey?.let(::AddTaskList),
        projectKey?.let { AddToProject(instanceDate, createTaskTimePair, projectKey) },
        AddTaskThisTime(instanceDate, createTaskTimePair).takeIf { showAddToTime },
    ).also { check(it.isNotEmpty()) }
}

private class AddTaskThisTime(
    val instanceDate: Date,
    val createTaskTimePair: TimePair,
) : BottomFabMenuDelegate.MenuDelegate.Item {

    override fun getText(context: Context) = context.getString(R.string.add_task_this_time)

    override fun onClick(activity: Activity) {
        activity.launchEditActivity(GroupListFragment.getHint(listOf(Tuple4(instanceDate, createTaskTimePair, null, null))))
    }
}

private class AddTaskList(val instanceKey: InstanceKey) : BottomFabMenuDelegate.MenuDelegate.Item {

    override fun getText(context: Context) = context.getString(R.string.addTaskList)

    override fun onClick(activity: Activity) =
        activity.launchEditActivity(EditParentHint.Instance(instanceKey, null))
}

private class AddToProject(
    val instanceDate: Date,
    val createTaskTimePair: TimePair,
    val projectKey: ProjectKey.Shared,
) : BottomFabMenuDelegate.MenuDelegate.Item {

    override fun getText(context: Context) = context.getString(R.string.addToProject)

    override fun onClick(activity: Activity) {
        activity.launchEditActivity(
            GroupListFragment.getHint(
                listOf(
                    Tuple4(
                        instanceDate,
                        createTaskTimePair,
                        projectKey,
                        null
                    )
                )
            )
        )
    }
}

private fun Activity.launchEditActivity(hint: EditParentHint) =
    startActivity(EditActivity.getParametersIntent(EditParameters.Create(hint)))