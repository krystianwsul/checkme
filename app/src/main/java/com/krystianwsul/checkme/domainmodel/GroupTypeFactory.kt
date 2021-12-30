package com.krystianwsul.checkme.domainmodel

import com.krystianwsul.checkme.gui.instances.ShowGroupActivity
import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.checkme.gui.instances.list.GroupListFragment
import com.krystianwsul.checkme.gui.instances.tree.NodeCollection
import com.krystianwsul.checkme.gui.instances.tree.NotDoneNode
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
        groupingMode: GroupType.GroupingMode,
    ) = GroupType.getGroupTypeTree(this, instanceDatas.map(GroupTypeFactory::InstanceDescriptor), groupingMode)
        .map { it.fix() }

    override fun createTime(
        timeStamp: TimeStamp,
        groupTypes: List<GroupType.TimeChild>,
        groupingMode: GroupType.GroupingMode.Time,
    ) = TimeBridge(timeStamp, groupTypes.map { it.fix() }, groupingMode::newShowGroupActivityParameters)

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
    ): GroupType.Project {
        val projectDetails = projectDescriptor.fix().projectDetails
        val instanceDatas = instanceDescriptors.map { it.fix().instanceData.copy(projectInfo = null) }

        return ProjectBridge(timeStamp, projectDetails, instanceDatas)
    }

    override fun createSingle(instanceDescriptor: GroupType.InstanceDescriptor): GroupType.Single {
        val instanceData = instanceDescriptor.fix().instanceData

        return SingleBridge(instanceData)
    }

    class InstanceDescriptor(val instanceData: GroupListDataWrapper.InstanceData) : GroupType.InstanceDescriptor {

        override val timeStamp get() = instanceData.instanceTimeStamp

        override val projectDescriptor = instanceData.projectInfo
            ?.projectDetails
            ?.let(GroupTypeFactory::ProjectDescriptor)
    }

    data class ProjectDescriptor(val projectDetails: DetailsNode.ProjectDetails) : GroupType.ProjectDescriptor

    sealed interface Bridge : Comparable<Bridge> {

        fun toContentDelegate(
            groupAdapter: GroupListFragment.GroupAdapter,
            indentation: Int,
            nodeCollection: NodeCollection,
        ): NotDoneNode.ContentDelegate
    }

    sealed interface SingleParent : Bridge, GroupType.SingleParent {

        val name: String get() = throw UnsupportedOperationException()
    }

    sealed interface TimeChild : Bridge, GroupType.TimeChild {

        val instanceKeys: Set<InstanceKey>
        val ordinal: Double
    }

    class TimeBridge(
        val timeStamp: TimeStamp,
        private val timeChildren: List<TimeChild>,
        private val newShowGroupActivityParameters: (TimeStamp) -> ShowGroupActivity.Parameters,
    ) : GroupType.Time, SingleParent {

        override fun toContentDelegate(
            groupAdapter: GroupListFragment.GroupAdapter,
            indentation: Int,
            nodeCollection: NodeCollection,
        ) = NotDoneNode.ContentDelegate.Group(
            groupAdapter,
            this,
            timeChildren.filterIsInstance<SingleBridge>().map { it.instanceData },
            indentation,
            nodeCollection,
            timeChildren,
            NotDoneNode.ContentDelegate.Group.Id.Time(timeStamp, timeChildren.flatMap { it.instanceKeys }.toSet()),
            NotDoneNode.ContentDelegate.Group.GroupRowsDelegate.Time(groupAdapter, timeStamp),
            newShowGroupActivityParameters(timeStamp),
            NotDoneNode.ContentDelegate.Group.CheckboxMode.INDENT,
        )

        override fun compareTo(other: Bridge): Int {
            return when (other) {
                is TimeBridge -> timeStamp.compareTo(timeStamp)
                is TimeProjectBridge -> timeStamp.compareTo(timeStamp)
                is ProjectBridge -> throw UnsupportedOperationException()
                is SingleBridge -> timeStamp.compareTo(other.instanceData.instanceTimeStamp)
            }
        }
    }

    class TimeProjectBridge(
        val timeStamp: TimeStamp,
        private val projectDetails: DetailsNode.ProjectDetails,
        val instanceDatas: List<GroupListDataWrapper.InstanceData>,
    ) : GroupType.TimeProject, SingleParent {

        override val name get() = projectDetails.name

        val instanceKeys = instanceDatas.map { it.instanceKey }.toSet()

        override fun toContentDelegate(
            groupAdapter: GroupListFragment.GroupAdapter,
            indentation: Int,
            nodeCollection: NodeCollection,
        ) = NotDoneNode.ContentDelegate.Group(
            groupAdapter,
            this,
            instanceDatas,
            indentation,
            nodeCollection,
            instanceDatas.map(GroupTypeFactory::SingleBridge),
            NotDoneNode.ContentDelegate.Group.Id.Project(timeStamp, instanceKeys, projectDetails.projectKey),
            NotDoneNode.ContentDelegate.Group.GroupRowsDelegate.Project(groupAdapter, timeStamp, projectDetails.name),
            ShowGroupActivity.Parameters.Project(timeStamp, projectDetails.projectKey),
            NotDoneNode.ContentDelegate.Group.CheckboxMode.CHECKBOX,
        )

        override fun compareTo(other: Bridge): Int {
            return when (other) {
                is TimeBridge -> timeStamp.compareTo(other.timeStamp)
                is TimeProjectBridge -> timeStamp.compareTo(other.timeStamp)
                is ProjectBridge -> throw UnsupportedOperationException()
                is SingleBridge -> timeStamp.compareTo(other.instanceData.instanceTimeStamp)
            }
        }
    }

    class ProjectBridge(
        val timeStamp: TimeStamp,
        val projectDetails: DetailsNode.ProjectDetails,
        val instanceDatas: List<GroupListDataWrapper.InstanceData>,
    ) : GroupType.Project, SingleParent, TimeChild {

        override val name get() = projectDetails.name

        override val instanceKeys = instanceDatas.map { it.instanceKey }.toSet()

        override val ordinal = projectDetails.projectKey.getOrdinal()

        override fun toContentDelegate(
            groupAdapter: GroupListFragment.GroupAdapter,
            indentation: Int,
            nodeCollection: NodeCollection,
        ) = NotDoneNode.ContentDelegate.Group(
            groupAdapter,
            this,
            instanceDatas,
            indentation,
            nodeCollection,
            instanceDatas.map(GroupTypeFactory::SingleBridge),
            NotDoneNode.ContentDelegate.Group.Id.Project(timeStamp, instanceKeys, projectDetails.projectKey),
            NotDoneNode.ContentDelegate.Group.GroupRowsDelegate.Project(groupAdapter, null, projectDetails.name),
            ShowGroupActivity.Parameters.Project(timeStamp, projectDetails.projectKey),
            NotDoneNode.ContentDelegate.Group.CheckboxMode.CHECKBOX,
        )

        override fun compareTo(other: Bridge): Int {
            return when (other) {
                is TimeBridge -> throw UnsupportedOperationException()
                is TimeProjectBridge -> throw UnsupportedOperationException()
                is TimeChild -> ordinal.compareTo(other.ordinal)
            }
        }
    }

    class SingleBridge(val instanceData: GroupListDataWrapper.InstanceData) : GroupType.Single, TimeChild {

        override val instanceKeys = setOf(instanceData.instanceKey)

        override val ordinal = instanceData.ordinal

        override fun toContentDelegate(
            groupAdapter: GroupListFragment.GroupAdapter,
            indentation: Int,
            nodeCollection: NodeCollection,
        ) = NotDoneNode.ContentDelegate.Instance(groupAdapter, this, indentation)

        override fun compareTo(other: Bridge): Int {
            return when (other) {
                is TimeBridge -> instanceData.instanceTimeStamp.compareTo(other.timeStamp)
                is TimeProjectBridge -> instanceData.instanceTimeStamp.compareTo(other.timeStamp)
                is ProjectBridge -> ordinal.compareTo(other.ordinal)
                is SingleBridge -> instanceData.compareTo(other.instanceData)
            }
        }
    }
}