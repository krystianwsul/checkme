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
            instanceDatas: List<GroupListDataWrapper.InstanceData>,
            groupingMode: NodeCollection.GroupingMode,
        ): List<GroupType> {
            if (instanceDatas.isEmpty()) return emptyList()

            return when (groupingMode) {
                NodeCollection.GroupingMode.TIME -> {
                    val timeGroups = instanceDatas.groupBy { it.instanceTimeStamp }

                    timeGroups.map { (timeStamp, instanceDatas) ->
                        check(instanceDatas.isNotEmpty())

                        // these are all instances at the same time
                        if (instanceDatas.size > 1) {
                            val projectDetails = instanceDatas.map { it.projectInfo?.projectDetails }
                                .distinct()
                                .singleOrNull()

                            projectDetails?.let {
                                TimeProject(timeStamp, projectDetails, instanceDatas)
                            } ?: Time(timeStamp, groupByProject(timeStamp, instanceDatas, true))
                        } else {
                            // if there's just one, there's our node
                            Single(instanceDatas.single())
                        }
                    }
                }
                NodeCollection.GroupingMode.PROJECT -> {
                    val timeStamp = instanceDatas.map { it.instanceTimeStamp }
                        .distinct()
                        .single()

                    groupByProject(timeStamp, instanceDatas, false)
                }
                NodeCollection.GroupingMode.NONE -> instanceDatas.map(GroupType::Single)
            }
        }

        private fun groupByProject(
            timeStamp: TimeStamp,
            instanceDatas: List<GroupListDataWrapper.InstanceData>,
            nested: Boolean,
        ): List<GroupType> {
            if (instanceDatas.isEmpty()) return emptyList()

            val projectGroups = instanceDatas.groupBy { it.projectInfo?.projectDetails }

            val groupTypesForShared = projectGroups.filterKeys { it != null }.map { (projectDetails, instanceDatas) ->
                check(instanceDatas.isNotEmpty())

                if (instanceDatas.size > 1) {
                    Project(timeStamp, projectDetails!!, instanceDatas, nested)
                } else {
                    Single(instanceDatas.single())
                }
            }

            val groupTypesForPrivate = projectGroups[null]?.map(GroupType::Single).orEmpty()

            return listOf(groupTypesForShared, groupTypesForPrivate).flatten()
        }
    }

    open val name: String get() = throw UnsupportedOperationException()

    protected abstract val allInstanceKeys: Set<InstanceKey>

    abstract fun toContentDelegate(
        groupAdapter: GroupListFragment.GroupAdapter,
        indentation: Int,
        nodeCollection: NodeCollection,
    ): NotDoneNode.ContentDelegate

    class Time(val timeStamp: TimeStamp, val groupTypes: List<GroupType>) : GroupType(), SingleParent {

        override val allInstanceKeys = groupTypes.flatMap { it.allInstanceKeys }.toSet()

        override fun toContentDelegate(
            groupAdapter: GroupListFragment.GroupAdapter,
            indentation: Int,
            nodeCollection: NodeCollection,
        ) = NotDoneNode.ContentDelegate.Group(
            groupAdapter,
            this,
            groupTypes.filterIsInstance<Single>().map { it.instanceData },
            indentation,
            nodeCollection,
            groupTypes,
            NotDoneNode.ContentDelegate.Group.Id.Time(timeStamp, allInstanceKeys),
            NotDoneNode.ContentDelegate.Group.GroupRowsDelegate.Time(groupAdapter, timeStamp),
            true,
            ShowGroupActivity.Parameters.Time(timeStamp),
        )

        override fun compareTo(other: GroupType): Int {
            return when (other) {
                is Time -> timeStamp.compareTo(timeStamp)
                is TimeProject -> timeStamp.compareTo(timeStamp)
                is Project -> throw UnsupportedOperationException()
                is Single -> timeStamp.compareTo(other.instanceData.instanceTimeStamp)
            }
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
        val timeStamp: TimeStamp,
        val projectDetails: DetailsNode.ProjectDetails,
        _instanceDatas: List<GroupListDataWrapper.InstanceData>,
    ) : GroupType(), SingleParent {

        private val instanceDatas = _instanceDatas.map { it.copy(projectInfo = null) }

        override val allInstanceKeys = instanceDatas.map { it.instanceKey }.toSet()

        override val name get() = projectDetails.name

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
            instanceDatas.map(::Single),
            NotDoneNode.ContentDelegate.Group.Id.Project(timeStamp, allInstanceKeys, projectDetails.projectKey),
            NotDoneNode.ContentDelegate.Group.GroupRowsDelegate.Project(
                groupAdapter,
                timeStamp,
                projectDetails.name,
                true,
            ),
            true, // todo group check param
            ShowGroupActivity.Parameters.Project(timeStamp, projectDetails.projectKey),
        )

        override fun compareTo(other: GroupType): Int {
            return when (other) {
                is Time -> timeStamp.compareTo(other.timeStamp)
                is TimeProject -> timeStamp.compareTo(other.timeStamp)
                is Project -> throw UnsupportedOperationException()
                is Single -> timeStamp.compareTo(other.instanceData.instanceTimeStamp)
            }
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
        val timeStamp: TimeStamp,
        val projectDetails: DetailsNode.ProjectDetails,
        _instanceDatas: List<GroupListDataWrapper.InstanceData>,
        private val nested: Boolean,
    ) : GroupType(), TimeChild, SingleParent {

        private val instanceDatas = _instanceDatas.map { it.copy(projectInfo = null) }

        override val allInstanceKeys = instanceDatas.map { it.instanceKey }.toSet()

        override val name get() = projectDetails.name

        override fun toContentDelegate(
            groupAdapter: GroupListFragment.GroupAdapter,
            indentation: Int,
            nodeCollection: NodeCollection,
        ) = NotDoneNode.ContentDelegate.Group(
            groupAdapter,
            this,
            instanceDatas,
            indentation + (if (nested) 1 else 0),
            nodeCollection,
            instanceDatas.map(::Single),
            NotDoneNode.ContentDelegate.Group.Id.Project(timeStamp, allInstanceKeys, projectDetails.projectKey),
            NotDoneNode.ContentDelegate.Group.GroupRowsDelegate.Project(
                groupAdapter,
                timeStamp,
                projectDetails.name,
                false, // todo group check usage (why am I passing in timeStamp?)
            ),
            !nested,
            ShowGroupActivity.Parameters.Project(timeStamp, projectDetails.projectKey),
        )

        override fun compareTo(other: GroupType): Int {
            return when (other) {
                is Time -> throw UnsupportedOperationException()
                is TimeProject -> throw UnsupportedOperationException()
                is Project -> projectDetails.projectKey.compareTo(other.projectDetails.projectKey)
                is Single -> -1
            }
        }
    }

    private interface TimeChild

    interface SingleParent

    class Single(val instanceData: GroupListDataWrapper.InstanceData) : GroupType(), TimeChild {

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
    }
}