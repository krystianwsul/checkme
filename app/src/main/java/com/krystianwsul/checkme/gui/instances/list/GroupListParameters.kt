package com.krystianwsul.checkme.gui.instances.list

import com.krystianwsul.checkme.gui.MainActivity

sealed class GroupListParameters(val draggable: Boolean = true) {

    abstract val dataId: Int
    abstract val immediate: Boolean
    abstract val groupListDataWrapper: GroupListDataWrapper

    open val showProgress: Boolean = false
    open val useDoneNode = true

    class All(
            override val dataId: Int,
            override val immediate: Boolean,
            override val groupListDataWrapper: GroupListDataWrapper,
            val position: Int,
            val timeRange: MainActivity.TimeRange,
            val differentPage: Boolean
    ) : GroupListParameters(false)

    class TimeStamp(
            override val dataId: Int,
            override val immediate: Boolean,
            override val groupListDataWrapper: GroupListDataWrapper,
            val timeStamp: com.krystianwsul.common.time.TimeStamp
    ) : GroupListParameters()

    class InstanceKey(
            override val dataId: Int,
            override val immediate: Boolean,
            override val groupListDataWrapper: GroupListDataWrapper,
            val instanceKey: com.krystianwsul.common.utils.InstanceKey
    ) : GroupListParameters()

    class InstanceKeys(
            override val dataId: Int,
            override val immediate: Boolean,
            override val groupListDataWrapper: GroupListDataWrapper
    ) : GroupListParameters(false)

    class TaskKey(
            override val dataId: Int,
            override val immediate: Boolean,
            override val groupListDataWrapper: GroupListDataWrapper,
            val taskKey: com.krystianwsul.common.utils.TaskKey,
            override val showProgress: Boolean
    ) : GroupListParameters(false) {

        override val useDoneNode = false
    }
}