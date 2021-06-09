package com.krystianwsul.checkme.gui.main

import android.content.Context
import com.krystianwsul.checkme.R
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

    override fun getItems() = listOf(AddTaskThisTime, AddTaskList)
}

private object AddTaskThisTime : BottomFabMenuDelegate.MenuDelegate.Item {

    override fun getText(context: Context) = context.getString(R.string.add_task_this_time)
}

private object AddTaskList : BottomFabMenuDelegate.MenuDelegate.Item {

    override fun getText(context: Context) = context.getString(R.string.addTaskList)
}