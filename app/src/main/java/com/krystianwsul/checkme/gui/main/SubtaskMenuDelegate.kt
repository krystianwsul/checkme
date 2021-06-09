package com.krystianwsul.checkme.gui.main

import android.app.Activity
import android.content.Context
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.gui.edit.EditActivity
import com.krystianwsul.checkme.gui.edit.EditParameters
import com.krystianwsul.checkme.gui.instances.list.GroupListFragment
import com.krystianwsul.checkme.gui.utils.BottomFabMenuDelegate
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.TimePair
import com.krystianwsul.common.utils.TaskKey
import kotlinx.parcelize.Parcelize

@Parcelize
class SubtaskMenuDelegate(
    val taskKey: TaskKey,
    val instanceDate: Date,
    val createTaskTimePair: TimePair,
) : BottomFabMenuDelegate.MenuDelegate {

    override fun getItems() = listOf(
        AddTaskList(taskKey),
        AddTaskThisTime(taskKey, instanceDate, createTaskTimePair),
    )
}

private class AddTaskThisTime(
    val taskKey: TaskKey,
    val instanceDate: Date,
    val createTaskTimePair: TimePair,
) : BottomFabMenuDelegate.MenuDelegate.Item {

    override fun getText(context: Context) = context.getString(R.string.add_task_this_time)

    override fun onClick(activity: Activity) {
        activity.launchEditActivity(GroupListFragment.getHint(listOf(Triple(instanceDate, createTaskTimePair, null))))
    }
}

private class AddTaskList(val taskKey: TaskKey) : BottomFabMenuDelegate.MenuDelegate.Item {

    override fun getText(context: Context) = context.getString(R.string.addTaskList)

    override fun onClick(activity: Activity) = activity.launchEditActivity(EditActivity.Hint.Task(taskKey))
}

private fun Activity.launchEditActivity(hint: EditActivity.Hint) =
    startActivity(EditActivity.getParametersIntent(EditParameters.Create(hint)))