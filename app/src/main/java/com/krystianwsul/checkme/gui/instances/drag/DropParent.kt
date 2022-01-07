package com.krystianwsul.checkme.gui.instances.drag

import com.krystianwsul.checkme.domainmodel.GroupTypeFactory
import com.krystianwsul.common.utils.InstanceKey

interface DropParent {

    fun canDropIntoParent(droppedTimeChild: GroupTypeFactory.TimeChild): Boolean

    // these have to be data classes because of GroupListDataWrapper

    data class TopLevel(val canDropOnTopLevel: Boolean) : DropParent {

        override fun canDropIntoParent(droppedTimeChild: GroupTypeFactory.TimeChild): Boolean {
            if (!canDropOnTopLevel) return false

            return when (droppedTimeChild) {
                is GroupTypeFactory.ProjectBridge -> true
                is GroupTypeFactory.SingleBridge -> droppedTimeChild.instanceData.isRootInstance
            }
        }
    }

    data class ParentInstance(val parentInstanceKey: InstanceKey) : DropParent {

        override fun canDropIntoParent(droppedTimeChild: GroupTypeFactory.TimeChild) = when (droppedTimeChild) {
            is GroupTypeFactory.ProjectBridge -> throw UnsupportedOperationException()
            is GroupTypeFactory.SingleBridge -> parentInstanceKey == droppedTimeChild.instanceData.parentInstanceKey!!
        }
    }
}