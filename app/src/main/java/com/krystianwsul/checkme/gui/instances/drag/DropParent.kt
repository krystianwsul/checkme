package com.krystianwsul.checkme.gui.instances.drag

import com.krystianwsul.checkme.domainmodel.GroupTypeFactory
import com.krystianwsul.checkme.domainmodel.updates.SetInstanceOrdinalDomainUpdate
import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.time.TimeStamp
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.common.utils.ProjectKey

interface DropParent {

    val newParentInfo: Instance.NewParentInfo

    fun canDropIntoParent(droppedTimeChild: GroupTypeFactory.TimeChild): Boolean

    // these have to be data classes because of GroupListDataWrapper

    data class TopLevel(val canDropOnTopLevel: Boolean) : DropParent {

        override val newParentInfo = Instance.NewParentInfo.TOP_LEVEL

        override fun canDropIntoParent(droppedTimeChild: GroupTypeFactory.TimeChild): Boolean {
            if (!canDropOnTopLevel) return false

            return when (droppedTimeChild) {
                is GroupTypeFactory.ProjectBridge -> true
                is GroupTypeFactory.SingleBridge -> droppedTimeChild.instanceData.isRootInstance
            }
        }
    }

    data class ParentInstance(val parentInstanceKey: InstanceKey) : DropParent {

        override val newParentInfo = Instance.NewParentInfo.NO_OP

        override fun canDropIntoParent(droppedTimeChild: GroupTypeFactory.TimeChild) = when (droppedTimeChild) {
            is GroupTypeFactory.ProjectBridge -> throw UnsupportedOperationException()
            is GroupTypeFactory.SingleBridge -> parentInstanceKey == droppedTimeChild.instanceData.parentInstanceKey!!
        }
    }

    data class Project(val timeStamp: TimeStamp, val projectKey: ProjectKey.Shared) : DropParent {

        override val newParentInfo = Instance.NewParentInfo.PROJECT

        override fun canDropIntoParent(droppedTimeChild: GroupTypeFactory.TimeChild) = when (droppedTimeChild) {
            is GroupTypeFactory.ProjectBridge -> false
            is GroupTypeFactory.SingleBridge -> droppedTimeChild.instanceData.let {
                timeStamp == it.instanceTimeStamp && projectKey == it.projectKey && it.isRootInstance
            }
        }
    }
}