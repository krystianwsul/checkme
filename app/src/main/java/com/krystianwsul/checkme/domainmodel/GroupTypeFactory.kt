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
    private val showDisplayText: Boolean,
    private val projectInfoMode: ProjectInfoMode,
    private val compareBy: SingleBridge.CompareBy,
    private val parentInstanceKey: InstanceKey?,
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
        val project = projectDescriptor.fix().project

        val singleBridges = instanceDescriptors.map { SingleBridge.createTimeProject(it.fix()) }

        return TimeProjectBridge(timeStamp, project.name, project.projectKey, singleBridges)
    }

    override fun createProject(
        timeStamp: TimeStamp,
        projectDescriptor: GroupType.ProjectDescriptor,
        instanceDescriptors: List<GroupType.InstanceDescriptor>,
    ): GroupType.Project {
        val fixedProjectDescriptor = projectDescriptor.fix()
        val project = fixedProjectDescriptor.project

        val fixedInstanceDescriptors = instanceDescriptors.map { it.fix() }

        val singleBridges = fixedInstanceDescriptors.map { SingleBridge.createProject(it) }

        val key = ProjectOrdinalManager.Key(
            fixedInstanceDescriptors.map {
                ProjectOrdinalManager.Key.Entry(it.instanceData.instanceKey, it.instanceDateTimePair)
            }.toSet(),
            fixedProjectDescriptor.parentInstance?.instanceKey,
        )

        return ProjectBridge(
            timeStamp,
            project.name,
            project.projectKey,
            singleBridges,
            projectOrdinalManagerProvider.getProjectOrdinalManager(project).getOrdinal(project, key),
            parentInstanceKey,
        )
    }

    override fun createTimeSingle(instanceDescriptor: GroupType.InstanceDescriptor) =
        SingleBridge.createTime(instanceDescriptor.fix(), projectInfoMode)

    override fun createTopLevelSingle(instanceDescriptor: GroupType.InstanceDescriptor) =
        SingleBridge.createTopLevel(instanceDescriptor.fix(), showDisplayText, projectInfoMode, compareBy)

    class InstanceDescriptor(
        val instanceData: GroupListDataWrapper.InstanceData,
        val instanceDateTimePair: DateTimePair,
        groupIntoProject: Boolean,
        val instance: Instance,
    ) : GroupType.InstanceDescriptor, Comparable<InstanceDescriptor> {

        override val timeStamp get() = instanceData.instanceTimeStamp

        override val projectDescriptor = instance.takeIf { groupIntoProject }
            ?.getProject()
            ?.let { it as? SharedProject } // group hack
            ?.takeIf { it != instance.parentInstance?.getProject() }
            ?.let { ProjectDescriptor(it, instance.parentInstance) }

        override fun compareTo(other: InstanceDescriptor) = instanceData.compareTo(other.instanceData)
    }

    data class ProjectDescriptor(val project: SharedProject, val parentInstance: Instance?) : GroupType.ProjectDescriptor

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
        override val name: String,
        private val projectKey: ProjectKey.Shared,
        private val singleBridges: List<SingleBridge>,
    ) : GroupType.TimeProject, SingleParent {


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
            NotDoneNode.ContentDelegate.Group.Id.Project(timeStamp, instanceKeys, projectKey),
            NotDoneNode.ContentDelegate.Group.GroupRowsDelegate.Project(groupAdapter, timeStamp, name),
            ShowGroupActivity.Parameters.Project(timeStamp, projectKey),
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
                timeStamp == it.instanceTimeStamp && projectKey == it.projectKey && it.isRootInstance
            }
        }
    }

    data class ProjectBridge(
        val timeStamp: TimeStamp,
        override val name: String,
        private val projectKey: ProjectKey.Shared,
        private val singeBridges: List<SingleBridge>,
        override val ordinal: Ordinal,
        val parentInstanceKey: InstanceKey?,
        private val dropParent: DropParent, // this could get set up to be inconsistent with parentInstanceKey; be careful
    ) : GroupType.Project, SingleParent, TimeChild, DropParent by dropParent {

        constructor(
            timeStamp: TimeStamp,
            name: String,
            projectKey: ProjectKey.Shared,
            singeBridges: List<SingleBridge>,
            ordinal: Ordinal,
            parentInstanceKey: InstanceKey?
        ) : this(
            timeStamp,
            name,
            projectKey,
            singeBridges,
            ordinal,
            parentInstanceKey,
            parentInstanceKey?.let {
                DropParent.InstanceProject(parentInstanceKey, projectKey)
            } ?: DropParent.Project(timeStamp, projectKey),
        )

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
            NotDoneNode.ContentDelegate.Group.Id.Project(timeStamp, instanceKeys, projectKey),
            NotDoneNode.ContentDelegate.Group.GroupRowsDelegate.Project(groupAdapter, null, name),
            parentInstanceKey?.let {
                ShowGroupActivity.Parameters.InstanceProject(projectKey, it)
            } ?: ShowGroupActivity.Parameters.Project(timeStamp, projectKey, false),
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
        val compareBy: CompareBy,
    ) : GroupType.Single,
        TimeChild,
        DropParent by DropParent.ParentInstance(instanceData.instanceKey, instanceData.projectKey) {

        companion object {

            fun createDone(
                instanceDescriptor: InstanceDescriptor,
                showDisplayText: Boolean,
                projectInfoMode: ProjectInfoMode,
            ) = SingleBridge(
                instanceDescriptor.instanceData,
                null,
                instanceDescriptor.takeIf { showDisplayText }
                    ?.instance
                    ?.getDisplayData()
                    ?.getDisplayText(),
                instanceDescriptor.instance.getProjectInfo(projectInfoMode),
                CompareBy.TIMESTAMP,
            )

            fun createTopLevel(
                instanceDescriptor: InstanceDescriptor,
                showDisplayText: Boolean,
                projectInfoMode: ProjectInfoMode,
                compareBy: CompareBy,
            ) = SingleBridge(
                instanceDescriptor.instanceData,
                false, // are directly in time
                instanceDescriptor.takeIf { showDisplayText }
                    ?.instance
                    ?.getDisplayData()
                    ?.getDisplayText(),
                instanceDescriptor.instance.getProjectInfo(projectInfoMode),
                compareBy,
            )

            fun createTime(instanceDescriptor: InstanceDescriptor, projectInfoMode: ProjectInfoMode) = SingleBridge(
                instanceDescriptor.instanceData,
                false,
                null,
                instanceDescriptor.instance.getProjectInfo(projectInfoMode),
                CompareBy.ORDINAL,
            )

            fun createTimeProject(instanceDescriptor: InstanceDescriptor) = SingleBridge(
                instanceDescriptor.instanceData,
                null, // can't be moved into time
                null,
                instanceDescriptor.instance.getProjectInfo(ProjectInfoMode.Hide),
                CompareBy.ORDINAL,
            )

            fun createProject(instanceDescriptor: InstanceDescriptor) = SingleBridge(
                instanceDescriptor.instanceData,
                true, // are nested in time
                null,
                instanceDescriptor.instance.getProjectInfo(ProjectInfoMode.Hide),
                CompareBy.ORDINAL,
            )
        }

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
                is SingleBridge -> when (compareBy) {
                    CompareBy.TIMESTAMP -> instanceData.compareTo(other.instanceData)
                    CompareBy.ORDINAL -> ordinal.compareTo(other.ordinal)
                }
            }
        }

        enum class CompareBy {

            TIMESTAMP, ORDINAL
        }
    }
}