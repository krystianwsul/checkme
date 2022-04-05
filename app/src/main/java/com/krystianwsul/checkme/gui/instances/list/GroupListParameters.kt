package com.krystianwsul.checkme.gui.instances.list

import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.viewmodels.DataId
import com.krystianwsul.common.utils.ProjectKey

sealed class GroupListParameters(val draggable: Boolean = true) {

    abstract val dataId: DataId
    abstract val immediate: Boolean
    abstract val groupListDataWrapper: GroupListDataWrapper

    open val showProgress: Boolean = false
    open val fabActionMode = FabActionMode.SUBTASK

    open val unscheduledFirst = false

    open val doneBeforeNotDone: Boolean = false

    data class All(
        override val dataId: DataId,
        override val immediate: Boolean,
        override val groupListDataWrapper: GroupListDataWrapper,
        val position: Int,
        val timeRange: Preferences.TimeRange,
        val differentPage: Boolean,
    ) : GroupListParameters(false) {

        override val fabActionMode = FabActionMode.BOTH
    }

    data class TimeStamp(
        override val dataId: DataId,
        override val immediate: Boolean,
        override val groupListDataWrapper: GroupListDataWrapper,
        val timeStamp: com.krystianwsul.common.time.TimeStamp,
        val projectKey: ProjectKey.Shared?,
    ) : GroupListParameters()

    data class InstanceKey(
        override val dataId: DataId,
        override val immediate: Boolean,
        override val groupListDataWrapper: GroupListDataWrapper,
        val instanceKey: com.krystianwsul.common.utils.InstanceKey,
    ) : GroupListParameters()

    data class InstanceKeys(
        override val dataId: DataId,
        override val immediate: Boolean,
        override val groupListDataWrapper: GroupListDataWrapper,
    ) : GroupListParameters(false)

    data class Parent(
        override val dataId: DataId,
        override val immediate: Boolean,
        override val groupListDataWrapper: GroupListDataWrapper,
        override val showProgress: Boolean,
        val projectKey: ProjectKey.Shared?,
        override val doneBeforeNotDone: Boolean,
    ) : GroupListParameters(false) {

        override val unscheduledFirst = true
    }

    data class Search(
        override val dataId: DataId,
        override val immediate: Boolean,
        override val groupListDataWrapper: GroupListDataWrapper,
        override val showProgress: Boolean,
    ) : GroupListParameters(false) {

        override val fabActionMode = FabActionMode.BOTH

        override val unscheduledFirst = true
    }

    enum class FabActionMode(val showSubtask: Boolean, val showTime: Boolean) {

        NONE(false, false),
        SUBTASK(true, false),
        BOTH(true, true)
    }
}