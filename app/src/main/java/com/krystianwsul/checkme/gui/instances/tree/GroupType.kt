package com.krystianwsul.checkme.gui.instances.tree

import com.krystianwsul.checkme.gui.instances.ShowGroupActivity
import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.checkme.gui.instances.list.GroupListFragment
import com.krystianwsul.checkme.gui.tree.DetailsNode
import com.krystianwsul.common.time.TimeStamp

sealed class GroupType {

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
                                Project(timeStamp, projectDetails, instanceDatas, false)
                            } ?: Time(timeStamp, groupByProject(timeStamp, instanceDatas))
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

                    groupByProject(timeStamp, instanceDatas)
                }
                NodeCollection.GroupingMode.NONE -> instanceDatas.map(GroupType::Single)
            }
        }

        private fun groupByProject(
            timeStamp: TimeStamp,
            instanceDatas: List<GroupListDataWrapper.InstanceData>,
        ): List<GroupType> {
            if (instanceDatas.isEmpty()) return emptyList()

            val projectGroups = instanceDatas.groupBy { it.projectInfo?.projectDetails }

            val groupTypesForShared = projectGroups.filterKeys { it != null }.map { (projectDetails, instanceDatas) ->
                check(instanceDatas.isNotEmpty())

                if (instanceDatas.size > 1) {
                    Project(timeStamp, projectDetails!!, instanceDatas, true)
                } else {
                    Single(instanceDatas.single())
                }
            }

            val groupTypesForPrivate = projectGroups[null]?.map(GroupType::Single).orEmpty()

            return listOf(groupTypesForShared, groupTypesForPrivate).flatten()
        }
    }

    open val name: String get() = throw UnsupportedOperationException()

    abstract fun toContentDelegate(
        groupAdapter: GroupListFragment.GroupAdapter,
        indentation: Int,
        nodeCollection: NodeCollection,
    ): NotDoneNode.ContentDelegate

    class Time(
        val timeStamp: TimeStamp,
        val groupTypes: List<GroupType>
    ) : GroupType() {

        override fun toContentDelegate(
            groupAdapter: GroupListFragment.GroupAdapter,
            indentation: Int,
            nodeCollection: NodeCollection,
        ) = NotDoneNode.ContentDelegate.Group(
            groupAdapter,
            this,
            groupTypes.filterIsInstance<Single>().map { it.instanceData },
            (groupTypes.first() as TimeChild).firstInstanceData,
            indentation,
            nodeCollection,
            groupTypes,
            NotDoneNode.ContentDelegate.Group.Id.Time(timeStamp),
            NotDoneNode.ContentDelegate.Group.GroupRowsDelegate.Time(groupAdapter, timeStamp),
            true,
            ShowGroupActivity.Parameters.Time(timeStamp),
        )
    }

    class Project(
        val timeStamp: TimeStamp,
        val projectDetails: DetailsNode.ProjectDetails,
        _instanceDatas: List<GroupListDataWrapper.InstanceData>,
        private val nested: Boolean,
    ) : GroupType(), TimeChild {

        private val instanceDatas = _instanceDatas.map { it.copy(projectInfo = null) }

        override val firstInstanceData = instanceDatas.first()

        override val name get() = projectDetails.name

        override fun toContentDelegate(
            groupAdapter: GroupListFragment.GroupAdapter,
            indentation: Int,
            nodeCollection: NodeCollection,
        ) = NotDoneNode.ContentDelegate.Group(
            groupAdapter,
            this,
            instanceDatas,
            firstInstanceData,
            indentation + (if (nested) 1 else 0),
            nodeCollection,
            instanceDatas.map(::Single),
            NotDoneNode.ContentDelegate.Group.Id.Project(timeStamp, projectDetails.projectKey),
            NotDoneNode.ContentDelegate.Group.GroupRowsDelegate.Project(
                groupAdapter,
                timeStamp,
                projectDetails.name,
                !nested,
            ),
            !nested,
            ShowGroupActivity.Parameters.Project(timeStamp, projectDetails.projectKey),
        )
    }

    private interface TimeChild {

        val firstInstanceData: GroupListDataWrapper.InstanceData
    }

    class Single(val instanceData: GroupListDataWrapper.InstanceData) : GroupType(), TimeChild {

        override val firstInstanceData = instanceData

        override fun toContentDelegate(
            groupAdapter: GroupListFragment.GroupAdapter,
            indentation: Int,
            nodeCollection: NodeCollection,
        ) = NotDoneNode.ContentDelegate.Instance(groupAdapter, instanceData, indentation)
    }
}