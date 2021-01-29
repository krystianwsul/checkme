package com.krystianwsul.checkme.gui.instances.list

import com.krystianwsul.checkme.Preferences
import com.krystianwsul.treeadapter.FilterCriteria

sealed class GroupListParameters(val draggable: Boolean = true) {

    abstract val dataId: Int
    abstract val immediate: Boolean
    abstract val groupListDataWrapper: GroupListDataWrapper

    open val showProgress: Boolean = false
    open val useDoneNode = true
    open val fabActionMode = FabActionMode.SUBTASK

    open val useGroups = false

    data class All(
            override val dataId: Int,
            override val immediate: Boolean,
            override val groupListDataWrapper: GroupListDataWrapper,
            val position: Int,
            val timeRange: Preferences.TimeRange,
            val differentPage: Boolean,
    ) : GroupListParameters(false) {

        override val fabActionMode = FabActionMode.BOTH

        override val useGroups = true
    }

    data class TimeStamp(
            override val dataId: Int,
            override val immediate: Boolean,
            override val groupListDataWrapper: GroupListDataWrapper,
            val timeStamp: com.krystianwsul.common.time.TimeStamp
    ) : GroupListParameters()

    data class InstanceKey(
            override val dataId: Int,
            override val immediate: Boolean,
            override val groupListDataWrapper: GroupListDataWrapper,
            val instanceKey: com.krystianwsul.common.utils.InstanceKey
    ) : GroupListParameters()

    data class InstanceKeys(
            override val dataId: Int,
            override val immediate: Boolean,
            override val groupListDataWrapper: GroupListDataWrapper
    ) : GroupListParameters(false)

    data class TaskKey(
            override val dataId: Int,
            override val immediate: Boolean,
            override val groupListDataWrapper: GroupListDataWrapper,
            val taskKey: com.krystianwsul.common.utils.TaskKey,
            override val showProgress: Boolean,
    ) : GroupListParameters(false) {

        override val useDoneNode = false
    }

    data class Search(
            override val dataId: Int,
            override val immediate: Boolean,
            override val groupListDataWrapper: GroupListDataWrapper,
            override val showProgress: Boolean,
            val filterCriteria: FilterCriteria,
    ) : GroupListParameters(false) {

        override val useDoneNode = false

        override val fabActionMode = FabActionMode.BOTH
    }

    enum class FabActionMode(val showSubtask: Boolean, val showTime: Boolean) {

        NONE(false, false),
        SUBTASK(true, false),
        BOTH(true, true)
    }
}