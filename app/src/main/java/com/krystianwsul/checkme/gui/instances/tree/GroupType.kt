package com.krystianwsul.checkme.gui.instances.tree

import com.krystianwsul.checkme.gui.instances.ShowGroupActivity
import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.checkme.gui.instances.list.GroupListFragment
import com.krystianwsul.checkme.gui.tree.DetailsNode
import com.krystianwsul.common.time.TimeStamp
import com.krystianwsul.common.utils.InstanceKey

sealed class GroupType : Comparable<GroupType> {

    companion object {

        fun getGroupTypeTree(
            bridgeFactory: BridgeFactory,
            instanceDatas: List<BridgeFactory.InstanceDescriptor>,
            groupingMode: NodeCollection.GroupingMode,
        ): List<GroupType> {
            if (instanceDatas.isEmpty()) return emptyList()

            return when (groupingMode) {
                NodeCollection.GroupingMode.TIME -> {
                    val timeGroups = instanceDatas.groupBy { it.timeStamp }

                    timeGroups.map { (timeStamp, instanceDescriptors) ->
                        check(instanceDescriptors.isNotEmpty())

                        // these are all instances at the same time
                        if (instanceDescriptors.size > 1) {
                            val projectDescriptor = instanceDescriptors.map { it.projectDescriptor }
                                .distinct()
                                .singleOrNull()

                            projectDescriptor?.let {
                                bridgeFactory.createTimeProject(timeStamp, projectDescriptor, instanceDescriptors)
                            } ?: bridgeFactory.createTime(
                                timeStamp,
                                groupByProject(bridgeFactory, timeStamp, instanceDescriptors, true)
                            )
                        } else {
                            // if there's just one, there's our node
                            bridgeFactory.createSingle(instanceDescriptors.single())
                        }
                    }
                }
                NodeCollection.GroupingMode.PROJECT -> {
                    val timeStamp = instanceDatas.map { it.timeStamp }
                        .distinct()
                        .single()

                    groupByProject(bridgeFactory, timeStamp, instanceDatas, false)
                }
                NodeCollection.GroupingMode.NONE -> instanceDatas.map(bridgeFactory::createSingle)
            }
        }

        private fun groupByProject(
            bridgeFactory: BridgeFactory,
            timeStamp: TimeStamp,
            instanceDescriptor: List<BridgeFactory.InstanceDescriptor>,
            nested: Boolean,
        ): List<GroupType> {
            if (instanceDescriptor.isEmpty()) return emptyList()

            val projectGroups = instanceDescriptor.groupBy { it.projectDescriptor }

            val groupTypesForShared = projectGroups.entries
                .filter { it.key != null }
                .map { (projectDescriptor, instanceDescriptors) ->
                    check(instanceDescriptors.isNotEmpty())

                    if (instanceDescriptors.size > 1) {
                        bridgeFactory.createProject(timeStamp, projectDescriptor!!, instanceDescriptors, nested)
                    } else {
                        bridgeFactory.createSingle(instanceDescriptors.single())
                    }
                }

            val groupTypesForPrivate = projectGroups[null]?.map(bridgeFactory::createSingle).orEmpty()

            return listOf(groupTypesForShared, groupTypesForPrivate).flatten()
        }
    }

    open val name: String get() = throw UnsupportedOperationException()

    abstract val allInstanceKeys: Set<InstanceKey>

    abstract fun toContentDelegate(
        groupAdapter: GroupListFragment.GroupAdapter,
        indentation: Int,
        nodeCollection: NodeCollection,
    ): NotDoneNode.ContentDelegate

    class Time(
        val bridge: Bridge,
        val timeStamp: TimeStamp,
        val groupTypes: List<GroupType>,
    ) : GroupType(), SingleParent {

        override val allInstanceKeys = groupTypes.flatMap { it.allInstanceKeys }.toSet()

        override fun toContentDelegate(
            groupAdapter: GroupListFragment.GroupAdapter,
            indentation: Int,
            nodeCollection: NodeCollection,
        ) = bridge.toContentDelegate(this, groupAdapter, indentation, nodeCollection)

        override fun compareTo(other: GroupType): Int {
            return when (other) {
                is Time -> timeStamp.compareTo(timeStamp)
                is TimeProject -> timeStamp.compareTo(timeStamp)
                is Project -> throw UnsupportedOperationException()
                is Single -> timeStamp.compareTo(other.instanceData.instanceTimeStamp)
            }
        }

        interface Bridge {

            fun toContentDelegate(
                time: Time,
                groupAdapter: GroupListFragment.GroupAdapter,
                indentation: Int,
                nodeCollection: NodeCollection,
            ): NotDoneNode.ContentDelegate.Group
        }
    }

    /**
     * This is a stand-alone item, when there are instances of exclusively one project at a given time.  Example list:
     *
     * Time
     *      Project
     *      Single
     * TimeProject
     *      Single
     * Single
     */
    class TimeProject(
        val bridge: Bridge,
        val timeStamp: TimeStamp,
        val projectDetails: DetailsNode.ProjectDetails,
        _instanceDatas: List<GroupListDataWrapper.InstanceData>,
    ) : GroupType(), SingleParent {

        val instanceDatas = _instanceDatas.map { it.copy(projectInfo = null) }

        override val allInstanceKeys = instanceDatas.map { it.instanceKey }.toSet()

        override val name get() = projectDetails.name

        override fun toContentDelegate(
            groupAdapter: GroupListFragment.GroupAdapter,
            indentation: Int,
            nodeCollection: NodeCollection,
        ) = bridge.toContentDelegate(this, groupAdapter, indentation, nodeCollection)

        override fun compareTo(other: GroupType): Int {
            return when (other) {
                is Time -> timeStamp.compareTo(other.timeStamp)
                is TimeProject -> timeStamp.compareTo(other.timeStamp)
                is Project -> throw UnsupportedOperationException()
                is Single -> timeStamp.compareTo(other.instanceData.instanceTimeStamp)
            }
        }

        interface Bridge {

            fun toContentDelegate(
                timeProject: TimeProject,
                groupAdapter: GroupListFragment.GroupAdapter,
                indentation: Int,
                nodeCollection: NodeCollection,
            ): NotDoneNode.ContentDelegate.Group
        }
    }

    /**
     * If nested, this is a child of Time on the main list:
     * Time
     *      Project
     *          Single
     *      Single
     *
     * Otherwise, it's a top-level node in a place where all instances have the same timeStamp, like ShowGroupActivity:
     * Project
     *      Single
     * Single
     */
    class Project(
        val bridge: Bridge,
        val timeStamp: TimeStamp,
        val projectDetails: DetailsNode.ProjectDetails,
        _instanceDatas: List<GroupListDataWrapper.InstanceData>,
        val nested: Boolean,
    ) : GroupType(), TimeChild, SingleParent {

        val instanceDatas = _instanceDatas.map { it.copy(projectInfo = null) }

        override val allInstanceKeys = instanceDatas.map { it.instanceKey }.toSet()

        override val name get() = projectDetails.name

        override fun toContentDelegate(
            groupAdapter: GroupListFragment.GroupAdapter,
            indentation: Int,
            nodeCollection: NodeCollection,
        ) = bridge.toContentDelegate(this, groupAdapter, indentation, nodeCollection)

        override fun compareTo(other: GroupType): Int {
            return when (other) {
                is Time -> throw UnsupportedOperationException()
                is TimeProject -> throw UnsupportedOperationException()
                is Project -> projectDetails.projectKey.compareTo(other.projectDetails.projectKey)
                is Single -> -1
            }
        }

        interface Bridge {

            fun toContentDelegate(
                project: Project,
                groupAdapter: GroupListFragment.GroupAdapter,
                indentation: Int,
                nodeCollection: NodeCollection,
            ): NotDoneNode.ContentDelegate.Group
        }
    }

    private interface TimeChild

    interface SingleParent

    class Single(val bridge: Bridge, val instanceData: GroupListDataWrapper.InstanceData) : GroupType(), TimeChild {

        override val allInstanceKeys = setOf(instanceData.instanceKey)

        override fun toContentDelegate(
            groupAdapter: GroupListFragment.GroupAdapter,
            indentation: Int,
            nodeCollection: NodeCollection,
        ) = NotDoneNode.ContentDelegate.Instance(groupAdapter, this, indentation)

        override fun compareTo(other: GroupType): Int {
            return when (other) {
                is Time -> instanceData.instanceTimeStamp.compareTo(other.timeStamp)
                is TimeProject -> instanceData.instanceTimeStamp.compareTo(other.timeStamp)
                is Project -> 1
                is Single -> instanceData.compareTo(other.instanceData)
            }
        }

        interface Bridge {

            fun toContentDelegate(
                single: Single,
                groupAdapter: GroupListFragment.GroupAdapter,
                indentation: Int,
                nodeCollection: NodeCollection,
            ): NotDoneNode.ContentDelegate.Instance
        }
    }

    interface BridgeFactory {

        fun createTime(timeStamp: TimeStamp, groupTypes: List<GroupType>): Time

        fun createTimeProject(
            timeStamp: TimeStamp,
            projectDescriptor: ProjectDescriptor,
            instanceDescriptors: List<InstanceDescriptor>,
        ): TimeProject

        fun createProject(
            timeStamp: TimeStamp,
            projectDescriptor: ProjectDescriptor,
            instanceDescriptors: List<InstanceDescriptor>,
            nested: Boolean,
        ): Project

        fun createSingle(instanceDescriptor: InstanceDescriptor): Single

        interface InstanceDescriptor {

            val timeStamp: TimeStamp

            val projectDescriptor: ProjectDescriptor?
        }

        interface ProjectDescriptor {

        }
    }

    object TreeAdapterBridgeFactory : BridgeFactory {

        fun getGroupTypeTree(
            instanceDatas: List<GroupListDataWrapper.InstanceData>,
            groupingMode: NodeCollection.GroupingMode,
        ) = getGroupTypeTree(this, instanceDatas.map(::InstanceDescriptor), groupingMode)

        override fun createTime(timeStamp: TimeStamp, groupTypes: List<GroupType>) =
            Time(TimeBridge(), timeStamp, groupTypes)

        override fun createTimeProject(
            timeStamp: TimeStamp,
            projectDescriptor: BridgeFactory.ProjectDescriptor,
            instanceDescriptors: List<BridgeFactory.InstanceDescriptor>
        ): TimeProject { // todo group use generics?
            return TimeProject(
                TimeProjectBridge(),
                timeStamp,
                (projectDescriptor as ProjectDescriptor).projectDetails,
                instanceDescriptors.map { (it as InstanceDescriptor).instanceData },
            )
        }

        override fun createProject(
            timeStamp: TimeStamp,
            projectDescriptor: BridgeFactory.ProjectDescriptor,
            instanceDescriptors: List<BridgeFactory.InstanceDescriptor>,
            nested: Boolean
        ): Project {
            return Project(
                ProjectBridge(),
                timeStamp,
                (projectDescriptor as ProjectDescriptor).projectDetails,
                instanceDescriptors.map { (it as InstanceDescriptor).instanceData },
                nested,
            )
        }

        override fun createSingle(instanceDescriptor: BridgeFactory.InstanceDescriptor): Single {
            return Single(SingleBridge(), (instanceDescriptor as InstanceDescriptor).instanceData)
        }

        class InstanceDescriptor(val instanceData: GroupListDataWrapper.InstanceData) : BridgeFactory.InstanceDescriptor {

            override val timeStamp get() = instanceData.instanceTimeStamp

            override val projectDescriptor = instanceData.projectInfo
                ?.projectDetails
                ?.let(::ProjectDescriptor)
        }

        data class ProjectDescriptor(val projectDetails: DetailsNode.ProjectDetails) : BridgeFactory.ProjectDescriptor

        class TimeBridge : Time.Bridge {

            override fun toContentDelegate(
                time: Time, // todo group eliminate this
                groupAdapter: GroupListFragment.GroupAdapter,
                indentation: Int,
                nodeCollection: NodeCollection,
            ) = NotDoneNode.ContentDelegate.Group(
                groupAdapter,
                time,
                time.groupTypes.filterIsInstance<Single>().map { it.instanceData },
                indentation,
                nodeCollection,
                time.groupTypes,
                NotDoneNode.ContentDelegate.Group.Id.Time(time.timeStamp, time.allInstanceKeys),
                NotDoneNode.ContentDelegate.Group.GroupRowsDelegate.Time(groupAdapter, time.timeStamp),
                true,
                ShowGroupActivity.Parameters.Time(time.timeStamp),
            )
        }

        class TimeProjectBridge : TimeProject.Bridge {

            override fun toContentDelegate(
                timeProject: TimeProject,
                groupAdapter: GroupListFragment.GroupAdapter,
                indentation: Int,
                nodeCollection: NodeCollection,
            ) = NotDoneNode.ContentDelegate.Group(
                groupAdapter,
                timeProject,
                timeProject.instanceDatas,
                indentation,
                nodeCollection,
                timeProject.instanceDatas.map { Single(SingleBridge(), it) },
                NotDoneNode.ContentDelegate.Group.Id.Project(
                    timeProject.timeStamp,
                    timeProject.allInstanceKeys,
                    timeProject.projectDetails.projectKey
                ),
                NotDoneNode.ContentDelegate.Group.GroupRowsDelegate.Project(
                    groupAdapter,
                    timeProject.timeStamp,
                    timeProject.projectDetails.name
                ),
                true,
                ShowGroupActivity.Parameters.Project(timeProject.timeStamp, timeProject.projectDetails.projectKey),
            )
        }

        class ProjectBridge : Project.Bridge {

            override fun toContentDelegate(
                project: Project,
                groupAdapter: GroupListFragment.GroupAdapter,
                indentation: Int,
                nodeCollection: NodeCollection,
            ) = NotDoneNode.ContentDelegate.Group(
                groupAdapter,
                project,
                project.instanceDatas,
                indentation + (if (project.nested) 1 else 0),
                nodeCollection,
                project.instanceDatas.map { Single(SingleBridge(), it) },
                NotDoneNode.ContentDelegate.Group.Id.Project(
                    project.timeStamp,
                    project.allInstanceKeys,
                    project.projectDetails.projectKey
                ),
                NotDoneNode.ContentDelegate.Group.GroupRowsDelegate.Project(groupAdapter, null, project.projectDetails.name),
                !project.nested,
                ShowGroupActivity.Parameters.Project(project.timeStamp, project.projectDetails.projectKey),
            )
        }

        class SingleBridge : Single.Bridge {

            override fun toContentDelegate(
                single: Single,
                groupAdapter: GroupListFragment.GroupAdapter,
                indentation: Int,
                nodeCollection: NodeCollection,
            ) = NotDoneNode.ContentDelegate.Instance(groupAdapter, single, indentation)
        }
    }
}