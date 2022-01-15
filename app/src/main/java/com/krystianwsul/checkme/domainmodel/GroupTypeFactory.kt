package com.krystianwsul.checkme.domainmodel

import com.krystianwsul.checkme.gui.instances.ShowGroupActivity
import com.krystianwsul.checkme.gui.instances.drag.DropParent
import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.checkme.gui.instances.list.GroupListFragment
import com.krystianwsul.checkme.gui.instances.tree.NodeCollection
import com.krystianwsul.checkme.gui.instances.tree.NotDoneNode
import com.krystianwsul.checkme.gui.tree.DetailsNode
import com.krystianwsul.checkme.utils.time.getDisplayText
import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.firebase.models.project.SharedProject
import com.krystianwsul.common.firebase.models.users.ProjectOrdinalManager
import com.krystianwsul.common.time.DateTimePair
import com.krystianwsul.common.time.TimeStamp
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.common.utils.Ordinal
import com.krystianwsul.common.utils.ProjectKey

class GroupTypeFactory(
    private val projectOrdinalManagerProvider: ProjectOrdinalManager.Provider,
    private val projectProvider: ProjectProvider,
    private val showDisplayText: Boolean,
) : GroupType.Factory {

    private fun GroupType.fix() = this as Bridge
    private fun GroupType.TimeChild.fix() = this as TimeChild
    private fun GroupType.InstanceDescriptor.fix() = this as InstanceDescriptor
    private fun GroupType.ProjectDescriptor.fix() = this as ProjectDescriptor

    fun getGroupTypeTree(
        instanceDescriptors: Collection<InstanceDescriptor>,
        groupingMode: GroupType.GroupingMode,
    ) = GroupType.getGroupTypeTree(this, instanceDescriptors, groupingMode)
        .map { it.fix() }

    override fun createTime(
        timeStamp: TimeStamp,
        groupTypes: List<GroupType.TimeChild>,
        groupingMode: GroupType.GroupingMode.Time,
    ) = TimeBridge(timeStamp, groupTypes.map { it.fix() }, groupingMode.newShowGroupActivityParameters(timeStamp))

    override fun createTimeProject(
        timeStamp: TimeStamp,
        projectDescriptor: GroupType.ProjectDescriptor,
        instanceDescriptors: List<GroupType.InstanceDescriptor>,
    ): GroupType.TimeProject {
        val projectDetails = projectDescriptor.fix().projectDetails

        val singleBridges = instanceDescriptors.map { SingleBridge.createGroupChild(it.fix(), false) }

        return TimeProjectBridge(timeStamp, projectDetails, singleBridges)
    }

    override fun createProject(
        timeStamp: TimeStamp,
        projectDescriptor: GroupType.ProjectDescriptor,
        instanceDescriptors: List<GroupType.InstanceDescriptor>,
    ): GroupType.Project {
        val projectDetails = projectDescriptor.fix().projectDetails

        val fixedInstanceDescriptors = instanceDescriptors.map { it.fix() }

        val singleBridges = fixedInstanceDescriptors.map { SingleBridge.createGroupChild(it, false) }

        val key = ProjectOrdinalManager.Key(
            fixedInstanceDescriptors.map {
                ProjectOrdinalManager.Key.Entry(it.instanceData.instanceKey, it.instanceDateTimePair)
            }.toSet()
        )

        val project = projectProvider.getProject(projectDetails.projectKey)

        return ProjectBridge(
            timeStamp,
            projectDetails,
            singleBridges,
            projectOrdinalManagerProvider.getProjectOrdinalManager(project).getOrdinal(project, key),
        )
    }

    override fun createSingle(instanceDescriptor: GroupType.InstanceDescriptor, nested: Boolean): GroupType.Single {
        val fixedInstanceDescriptor = instanceDescriptor.fix()

        return if (nested)
            SingleBridge.createGroupChild(fixedInstanceDescriptor)
        else
            SingleBridge.createTopLevelNotDone(fixedInstanceDescriptor, showDisplayText)
    }

    class InstanceDescriptor(
        val instanceData: GroupListDataWrapper.InstanceData,
        val instanceDateTimePair: DateTimePair,
        override val groupIntoProject: Boolean,
        val instance: Instance,
    ) : GroupType.InstanceDescriptor, Comparable<InstanceDescriptor> {

        override val timeStamp get() = instanceData.instanceTimeStamp

        override val projectDescriptor = instanceData.projectInfo
            ?.projectDetails
            ?.let(GroupTypeFactory::ProjectDescriptor)

        override fun compareTo(other: InstanceDescriptor) = instanceData.compareTo(other.instanceData)
    }

    data class ProjectDescriptor(val projectDetails: DetailsNode.ProjectDetails) : GroupType.ProjectDescriptor

    sealed interface Bridge : Comparable<Bridge>, DropParent {

        fun toContentDelegate(
            groupAdapter: GroupListFragment.GroupAdapter,
            indentation: Int,
            nodeCollection: NodeCollection,
        ): NotDoneNode.ContentDelegate
    }

    sealed interface SingleParent : Bridge, GroupType.SingleParent {

        val name: String get() = throw UnsupportedOperationException()

        val sortable: Boolean get() = false
        val ordinal: Ordinal get() = throw UnsupportedOperationException()
    }

    sealed interface TimeChild : Bridge, GroupType.TimeChild {

        val instanceKeys: Set<InstanceKey>
        val ordinal: Ordinal
    }

    data class TimeBridge(
        val timeStamp: TimeStamp,
        private val timeChildren: List<TimeChild>,
        private val showGroupActivityParameters: ShowGroupActivity.Parameters,
    ) : GroupType.Time, SingleParent {

        override fun getNewParentInfo(isGroupedInProject: Boolean?) =
            if (isGroupedInProject!!) Instance.NewParentInfo.TOP_LEVEL else Instance.NewParentInfo.NO_OP

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
            showGroupActivityParameters,
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

        override fun canDropIntoParent(droppedTimeChild: TimeChild) = when (droppedTimeChild) {
            is ProjectBridge -> timeStamp == droppedTimeChild.timeStamp
            is SingleBridge -> droppedTimeChild.instanceData.let { timeStamp == it.instanceTimeStamp && it.isRootInstance }
        }
    }

    data class TimeProjectBridge(
        val timeStamp: TimeStamp,
        private val projectDetails: DetailsNode.ProjectDetails,
        private val singleBridges: List<SingleBridge>,
    ) : GroupType.TimeProject, SingleParent {

        override val name get() = projectDetails.name

        private val instanceDatas = singleBridges.map { it.instanceData }

        val instanceKeys = instanceDatas.map { it.instanceKey }.toSet()

        override fun getNewParentInfo(isGroupedInProject: Boolean?) = Instance.NewParentInfo.NO_OP

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
            singleBridges,
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

        override fun canDropIntoParent(droppedTimeChild: TimeChild) = when (droppedTimeChild) {
            is ProjectBridge -> throw UnsupportedOperationException()
            is SingleBridge -> droppedTimeChild.instanceData.let {
                timeStamp == it.instanceTimeStamp && projectDetails.projectKey == it.projectKey && it.isRootInstance
            }
        }
    }

    data class ProjectBridge(
        val timeStamp: TimeStamp,
        private val projectDetails: DetailsNode.ProjectDetails,
        private val singeBridges: List<SingleBridge>,
        override val ordinal: Ordinal,
    ) : GroupType.Project, SingleParent, TimeChild, DropParent by DropParent.Project(timeStamp, projectDetails.projectKey) {

        override val name get() = projectDetails.name

        private val instanceDatas = singeBridges.map { it.instanceData }

        override val instanceKeys = instanceDatas.map { it.instanceKey }.toSet()

        override val sortable = true

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
            singeBridges,
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

    data class SingleBridge(
        // don't use constructor directly
        val instanceData: GroupListDataWrapper.InstanceData,
        val isGroupedInProject: Boolean?, // null means throw an error if you need it
        val displayText: String?,
        val projectInfo: DetailsNode.ProjectInfo?,
    ) : GroupType.Single, TimeChild {

        companion object {

            fun createDone(instanceDescriptor: InstanceDescriptor, showDisplayText: Boolean) = SingleBridge(
                instanceDescriptor.instanceData,
                false,
                instanceDescriptor.takeIf { showDisplayText }
                    ?.instance
                    ?.getDisplayData()
                    ?.getDisplayText(),
                instanceDescriptor.instanceData.projectInfo,
            )

            fun createGroupChild(instanceDescriptor: InstanceDescriptor, includeProjectDetails: Boolean = true) =
                SingleBridge(
                    instanceDescriptor.instanceData,
                    false,
                    null,
                    instanceDescriptor.instanceData
                        .let {
                            if (includeProjectDetails)
                                it
                            else
                                it.stripProjectDetails()
                        }
                        .projectInfo,
                )

            fun createTopLevelNotDone(instanceDescriptor: InstanceDescriptor, showDisplayText: Boolean) = SingleBridge(
                instanceDescriptor.instanceData,
                false,
                instanceDescriptor.takeIf { showDisplayText }
                    ?.instance
                    ?.getDisplayData()
                    ?.getDisplayText(),
                instanceDescriptor.instanceData.projectInfo,
            )
        }

        override val instanceKeys = setOf(instanceData.instanceKey)

        init {
            if (instanceData.name.contains("like") && isGroupedInProject == null) throw Exception()
        }

        override val ordinal = instanceData.ordinal

        override fun getNewParentInfo(isGroupedInProject: Boolean?) = Instance.NewParentInfo.NO_OP

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

        override fun canDropIntoParent(droppedTimeChild: TimeChild) = when (droppedTimeChild) {
            is ProjectBridge -> false
            is SingleBridge -> instanceData.instanceKey == droppedTimeChild.instanceData.parentInstanceKey
        }
    }

    fun interface ProjectProvider {

        fun getProject(projectKey: ProjectKey.Shared): SharedProject
    }
}