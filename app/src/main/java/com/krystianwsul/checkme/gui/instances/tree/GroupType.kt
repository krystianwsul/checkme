package com.krystianwsul.checkme.gui.instances.tree

import com.krystianwsul.checkme.gui.instances.ShowGroupActivity
import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.checkme.gui.instances.list.GroupListFragment
import com.krystianwsul.checkme.gui.tree.DetailsNode
import com.krystianwsul.common.time.TimeStamp
import com.krystianwsul.common.utils.InstanceKey

sealed class GroupType {

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

    abstract val allInstanceKeys: Set<InstanceKey>

    abstract val bridge: BridgeFactory.Bridge

    abstract fun toContentDelegate(
        groupAdapter: GroupListFragment.GroupAdapter,
        indentation: Int,
        nodeCollection: NodeCollection,
    ): NotDoneNode.ContentDelegate

    class Time(
        override val bridge: Bridge,
        val timeStamp: TimeStamp,
        val groupTypes: List<GroupType>,
    ) : GroupType(), SingleParent {

        override val allInstanceKeys = groupTypes.flatMap { it.allInstanceKeys }.toSet()

        override fun toContentDelegate(
            groupAdapter: GroupListFragment.GroupAdapter,
            indentation: Int,
            nodeCollection: NodeCollection,
        ) = bridge.toContentDelegate(this, groupAdapter, indentation, nodeCollection)

        interface Bridge : BridgeFactory.Bridge {

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
        override val bridge: Bridge,
        val timeStamp: TimeStamp,
        val projectDetails: DetailsNode.ProjectDetails,
        val instanceDatas: List<GroupListDataWrapper.InstanceData>,
    ) : GroupType(), SingleParent {

        override val allInstanceKeys = instanceDatas.map { it.instanceKey }.toSet()

        override fun toContentDelegate(
            groupAdapter: GroupListFragment.GroupAdapter,
            indentation: Int,
            nodeCollection: NodeCollection,
        ) = bridge.toContentDelegate(this, groupAdapter, indentation, nodeCollection)

        interface Bridge : BridgeFactory.Bridge {

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
        override val bridge: Bridge,
        val timeStamp: TimeStamp,
        val projectDetails: DetailsNode.ProjectDetails,
        val instanceDatas: List<GroupListDataWrapper.InstanceData>,
        val nested: Boolean,
    ) : GroupType(), TimeChild, SingleParent {

        override val allInstanceKeys = instanceDatas.map { it.instanceKey }.toSet()

        override fun toContentDelegate(
            groupAdapter: GroupListFragment.GroupAdapter,
            indentation: Int,
            nodeCollection: NodeCollection,
        ) = bridge.toContentDelegate(this, groupAdapter, indentation, nodeCollection)

        interface Bridge : BridgeFactory.Bridge {

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

    class Single(override val bridge: Bridge, val instanceData: GroupListDataWrapper.InstanceData) : GroupType(), TimeChild {

        override val allInstanceKeys = setOf(instanceData.instanceKey)

        override fun toContentDelegate(
            groupAdapter: GroupListFragment.GroupAdapter,
            indentation: Int,
            nodeCollection: NodeCollection,
        ) = bridge.toContentDelegate(this, groupAdapter, indentation, nodeCollection)

        interface Bridge : BridgeFactory.Bridge {

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

        interface Bridge
    }

    object TreeAdapterBridgeFactory : BridgeFactory {

        fun getGroupTypeTree(
            instanceDatas: List<GroupListDataWrapper.InstanceData>,
            groupingMode: NodeCollection.GroupingMode,
        ) = getGroupTypeTree(this, instanceDatas.map(::InstanceDescriptor), groupingMode)

        override fun createTime(timeStamp: TimeStamp, groupTypes: List<GroupType>) =
            Time(TimeBridge(timeStamp, groupTypes), timeStamp, groupTypes)

        override fun createTimeProject(
            timeStamp: TimeStamp,
            projectDescriptor: BridgeFactory.ProjectDescriptor,
            instanceDescriptors: List<BridgeFactory.InstanceDescriptor>
        ): TimeProject { // todo group use generics?
            val projectDetails = (projectDescriptor as ProjectDescriptor).projectDetails
            val instanceDatas = instanceDescriptors.map { (it as InstanceDescriptor).instanceData.copy(projectInfo = null) }

            return TimeProject(
                TimeProjectBridge(timeStamp, projectDetails, instanceDatas),
                timeStamp,
                projectDetails,
                instanceDatas,
            )
        }

        override fun createProject(
            timeStamp: TimeStamp,
            projectDescriptor: BridgeFactory.ProjectDescriptor,
            instanceDescriptors: List<BridgeFactory.InstanceDescriptor>,
            nested: Boolean
        ): Project {
            val projectDetails = (projectDescriptor as ProjectDescriptor).projectDetails
            val instanceDatas = instanceDescriptors.map { (it as InstanceDescriptor).instanceData.copy(projectInfo = null) }

            return Project(
                ProjectBridge(timeStamp, projectDetails, instanceDatas, nested),
                timeStamp,
                projectDetails,
                instanceDatas,
                nested,
            )
        }

        override fun createSingle(instanceDescriptor: BridgeFactory.InstanceDescriptor): Single {
            val instanceData = (instanceDescriptor as InstanceDescriptor).instanceData

            return Single(SingleBridge(instanceData), instanceData)
        }

        class InstanceDescriptor(val instanceData: GroupListDataWrapper.InstanceData) : BridgeFactory.InstanceDescriptor {

            override val timeStamp get() = instanceData.instanceTimeStamp

            override val projectDescriptor = instanceData.projectInfo
                ?.projectDetails
                ?.let(::ProjectDescriptor)
        }

        data class ProjectDescriptor(val projectDetails: DetailsNode.ProjectDetails) : BridgeFactory.ProjectDescriptor

        sealed interface Bridge : Comparable<Bridge>

        sealed interface SingleParent : Bridge {

            val name: String get() = throw UnsupportedOperationException()
        }

        class TimeBridge(
            val timeStamp: TimeStamp,
            val groupTypes: List<GroupType>,
        ) : Time.Bridge, SingleParent {

            override fun toContentDelegate(
                time: Time, // todo group eliminate this
                groupAdapter: GroupListFragment.GroupAdapter,
                indentation: Int,
                nodeCollection: NodeCollection,
            ) = NotDoneNode.ContentDelegate.Group(
                groupAdapter,
                this,
                time.groupTypes.filterIsInstance<Single>().map { it.instanceData },
                indentation,
                nodeCollection,
                time.groupTypes,
                NotDoneNode.ContentDelegate.Group.Id.Time(time.timeStamp, time.allInstanceKeys),
                NotDoneNode.ContentDelegate.Group.GroupRowsDelegate.Time(groupAdapter, time.timeStamp),
                true,
                ShowGroupActivity.Parameters.Time(time.timeStamp),
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
            val projectDetails: DetailsNode.ProjectDetails,
            val instanceDatas: List<GroupListDataWrapper.InstanceData>,
        ) : TimeProject.Bridge, SingleParent {

            override val name get() = projectDetails.name

            override fun toContentDelegate(
                timeProject: TimeProject,
                groupAdapter: GroupListFragment.GroupAdapter,
                indentation: Int,
                nodeCollection: NodeCollection,
            ) = NotDoneNode.ContentDelegate.Group(
                groupAdapter,
                this,
                timeProject.instanceDatas,
                indentation,
                nodeCollection,
                timeProject.instanceDatas.map { Single(SingleBridge(it), it) },
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
            val nested: Boolean,
        ) : Project.Bridge, SingleParent {

            override val name get() = projectDetails.name

            override fun toContentDelegate(
                project: Project,
                groupAdapter: GroupListFragment.GroupAdapter,
                indentation: Int,
                nodeCollection: NodeCollection,
            ) = NotDoneNode.ContentDelegate.Group(
                groupAdapter,
                this,
                project.instanceDatas,
                indentation + (if (project.nested) 1 else 0),
                nodeCollection,
                project.instanceDatas.map { Single(SingleBridge(it), it) },
                NotDoneNode.ContentDelegate.Group.Id.Project(
                    project.timeStamp,
                    project.allInstanceKeys,
                    project.projectDetails.projectKey
                ),
                NotDoneNode.ContentDelegate.Group.GroupRowsDelegate.Project(groupAdapter, null, project.projectDetails.name),
                !project.nested,
                ShowGroupActivity.Parameters.Project(project.timeStamp, project.projectDetails.projectKey),
            )

            override fun compareTo(other: Bridge): Int {
                return when (other) {
                    is TimeBridge -> throw UnsupportedOperationException()
                    is TimeProjectBridge -> throw UnsupportedOperationException()
                    is ProjectBridge -> projectDetails.projectKey.compareTo(other.projectDetails.projectKey)
                    is SingleBridge -> -1
                }
            }
        }

        class SingleBridge(val instanceData: GroupListDataWrapper.InstanceData) : Single.Bridge, Bridge {

            override fun toContentDelegate(
                single: Single,
                groupAdapter: GroupListFragment.GroupAdapter,
                indentation: Int,
                nodeCollection: NodeCollection,
            ) = NotDoneNode.ContentDelegate.Instance(groupAdapter, this, indentation)

            override fun compareTo(other: Bridge): Int {
                return when (other) {
                    is TimeBridge -> instanceData.instanceTimeStamp.compareTo(other.timeStamp)
                    is TimeProjectBridge -> instanceData.instanceTimeStamp.compareTo(other.timeStamp)
                    is ProjectBridge -> 1
                    is SingleBridge -> instanceData.compareTo(other.instanceData)
                }
            }
        }
    }
}