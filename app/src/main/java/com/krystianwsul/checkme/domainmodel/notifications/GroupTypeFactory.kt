package com.krystianwsul.checkme.domainmodel.notifications

import com.krystianwsul.checkme.domainmodel.GroupType
import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.checkme.gui.instances.tree.NodeCollection
import com.krystianwsul.checkme.gui.tree.DetailsNode
import com.krystianwsul.common.time.TimeStamp
import com.krystianwsul.common.utils.InstanceKey

object GroupTypeFactory : GroupType.Factory {

    private fun GroupType.fix() = this as Bridge
    private fun GroupType.TimeChild.fix() = this as TimeChild
    private fun GroupType.InstanceDescriptor.fix() = this as InstanceDescriptor
    private fun GroupType.ProjectDescriptor.fix() = this as ProjectDescriptor

    fun getGroupTypeTree(
        instanceDatas: List<GroupListDataWrapper.InstanceData>,
        groupingMode: NodeCollection.GroupingMode,
    ) = GroupType.getGroupTypeTree(this, instanceDatas.map(::InstanceDescriptor), groupingMode).map { it.fix() }

    override fun createTime(timeStamp: TimeStamp, groupTypes: List<GroupType.TimeChild>) =
        TimeBridge(timeStamp, groupTypes.map { it.fix() })

    override fun createTimeProject(
        timeStamp: TimeStamp,
        projectDescriptor: GroupType.ProjectDescriptor,
        instanceDescriptors: List<GroupType.InstanceDescriptor>,
    ): GroupType.TimeProject {
        val projectDetails = projectDescriptor.fix().projectDetails
        val instanceDatas = instanceDescriptors.map { it.fix().instanceData.copy(projectInfo = null) }

        return TimeProjectBridge(timeStamp, projectDetails, instanceDatas)
    }

    override fun createProject(
        timeStamp: TimeStamp,
        projectDescriptor: GroupType.ProjectDescriptor,
        instanceDescriptors: List<GroupType.InstanceDescriptor>,
        nested: Boolean,
    ): GroupType.Project {
        val projectDetails = projectDescriptor.fix().projectDetails
        val instanceDatas = instanceDescriptors.map { it.fix().instanceData.copy(projectInfo = null) }

        return ProjectBridge(timeStamp, projectDetails, instanceDatas, nested)
    }

    override fun createSingle(instanceDescriptor: GroupType.InstanceDescriptor): GroupType.Single {
        val instanceData = instanceDescriptor.fix().instanceData

        return SingleBridge(instanceData)
    }

    class InstanceDescriptor(val instanceData: GroupListDataWrapper.InstanceData) : GroupType.InstanceDescriptor {

        override val timeStamp get() = instanceData.instanceTimeStamp

        override val projectDescriptor = instanceData.projectInfo
            ?.projectDetails
            ?.let(::ProjectDescriptor)
    }

    data class ProjectDescriptor(val projectDetails: DetailsNode.ProjectDetails) : GroupType.ProjectDescriptor

    sealed interface Bridge

    sealed interface SingleParent : Bridge, GroupType.SingleParent

    sealed interface TimeChild : Bridge, GroupType.TimeChild {

        val instanceKeys: Set<InstanceKey>
    }

    class TimeBridge(val timeStamp: TimeStamp, private val timeChildren: List<TimeChild>) : GroupType.Time, SingleParent

    class TimeProjectBridge(
        val timeStamp: TimeStamp,
        val projectDetails: DetailsNode.ProjectDetails,
        val instanceDatas: List<GroupListDataWrapper.InstanceData>,
    ) : GroupType.TimeProject, SingleParent {

        val instanceKeys = instanceDatas.map { it.instanceKey }.toSet()
    }

    class ProjectBridge(
        val timeStamp: TimeStamp,
        val projectDetails: DetailsNode.ProjectDetails,
        val instanceDatas: List<GroupListDataWrapper.InstanceData>,
        private val nested: Boolean,
    ) : GroupType.Project, SingleParent, TimeChild {

        override val instanceKeys = instanceDatas.map { it.instanceKey }.toSet()
    }

    class SingleBridge(val instanceData: GroupListDataWrapper.InstanceData) : GroupType.Single, TimeChild {

        override val instanceKeys = setOf(instanceData.instanceKey)
    }
}