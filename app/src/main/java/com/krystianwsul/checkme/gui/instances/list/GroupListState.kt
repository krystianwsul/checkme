package com.krystianwsul.checkme.gui.instances.list

import android.os.Parcelable
import com.krystianwsul.common.time.TimeStamp
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.common.utils.TaskKey
import kotlinx.parcelize.Parcelize

@Parcelize
data class GroupListState(
        val doneExpanded: Boolean = false,
        val expandedGroups: List<TimeStamp> = listOf(),
        val expandedInstances: Map<InstanceKey, Boolean> = mapOf(),
        val unscheduledExpanded: Boolean = false,
        val expandedTaskKeys: List<TaskKey> = listOf(),
        val selectedInstances: List<InstanceKey> = listOf(),
        val selectedGroups: List<Long> = listOf(),
        val selectedTaskKeys: List<TaskKey> = listOf()
) : Parcelable