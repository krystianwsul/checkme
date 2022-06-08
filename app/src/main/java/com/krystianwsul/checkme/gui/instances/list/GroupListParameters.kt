package com.krystianwsul.checkme.gui.instances.list

import com.krystianwsul.checkme.gui.edit.EditParentHint
import com.krystianwsul.checkme.viewmodels.DataId
import com.krystianwsul.common.time.TimePair
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
        val differentPage: Boolean,
    ) : GroupListParameters(false) {

        override val fabActionMode = FabActionMode.BOTH
    }

    data class TimeStamp(
        override val dataId: DataId,
        override val immediate: Boolean,
        override val groupListDataWrapper: GroupListDataWrapper,
        val fabData: FabData,
    ) : GroupListParameters() {

        sealed class FabData {

            abstract fun toEditParentHint(): EditParentHint

            data class TimeBased(
                val timeStamp: com.krystianwsul.common.time.TimeStamp,
                val projectKey: ProjectKey.Shared?,
            ) : FabData() {

                override fun toEditParentHint() = EditParentHint.Schedule(
                    timeStamp.date,
                    TimePair(timeStamp.hourMinute),
                    projectKey,
                )
            }

            data class InstanceProject(
                val parentInstanceKey: com.krystianwsul.common.utils.InstanceKey,
                val projectKey: ProjectKey.Shared,
            ) : FabData() {

                override fun toEditParentHint() = EditParentHint.Instance(parentInstanceKey, projectKey)
            }
        }
    }

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