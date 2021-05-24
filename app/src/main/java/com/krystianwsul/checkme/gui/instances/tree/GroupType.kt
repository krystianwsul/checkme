package com.krystianwsul.checkme.gui.instances.tree

import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.checkme.gui.instances.list.GroupListFragment
import com.krystianwsul.common.time.TimeStamp
import com.krystianwsul.common.utils.ProjectKey

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
                            // if there are multiple instances, we want to determine if they belong to a single shared project
                            val projectKey = instanceDatas.map { it.projectInfo?.projectDetails?.projectKey }
                                .distinct()
                                .singleOrNull()

                            projectKey?.let {
                                Project(timeStamp, projectKey, instanceDatas, true)
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

            val projectGroups = instanceDatas.groupBy { it.projectInfo?.projectDetails?.projectKey }

            val groupTypesForShared = projectGroups.filterKeys { it != null }.map { (projectKey, instanceDatas) ->
                check(instanceDatas.isNotEmpty())

                if (instanceDatas.size > 1) {
                    Project(timeStamp, projectKey!!, instanceDatas, false)
                } else {
                    Single(instanceDatas.single())
                }
            }

            val groupTypesForPrivate = projectGroups[null]?.map(GroupType::Single).orEmpty()

            return listOf(groupTypesForShared, groupTypesForPrivate).flatten()
        }
    }

    abstract val instanceDatas: List<GroupListDataWrapper.InstanceData> // todo project later InstanceDatas

    abstract fun toContentDelegate(
        groupAdapter: GroupListFragment.GroupAdapter,
        indentation: Int,
        nodeCollection: NodeCollection,
    ): NotDoneNode.ContentDelegate

    data class Time(
        val timeStamp: TimeStamp,
        val groupTypes: List<GroupType>
    ) : GroupType() {

        override val instanceDatas = groupTypes.flatMap { it.instanceDatas }

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
            NodeCollection.GroupingMode.PROJECT,
            nodeCollection,
            groupTypes,
            NotDoneNode.ContentDelegate.Group.Id.Time(timeStamp),
        )
    }

    data class Project(
        val timeStamp: TimeStamp,
        val projectKey: ProjectKey.Shared,
        override val instanceDatas: List<GroupListDataWrapper.InstanceData>,
        val showTime: Boolean,
    ) : GroupType(), TimeChild {

        override val firstInstanceData = instanceDatas.first()

        override fun toContentDelegate(
            groupAdapter: GroupListFragment.GroupAdapter,
            indentation: Int,
            nodeCollection: NodeCollection,
        ) = NotDoneNode.ContentDelegate.Group(
            groupAdapter,
            this,
            instanceDatas,
            firstInstanceData,
            indentation,
            NodeCollection.GroupingMode.NONE,
            nodeCollection,
            instanceDatas.map(::Single),
            NotDoneNode.ContentDelegate.Group.Id.Project(timeStamp, projectKey),
        ) // todo project new delegate
    }

    private interface TimeChild {

        val firstInstanceData: GroupListDataWrapper.InstanceData
    }

    data class Single(val instanceData: GroupListDataWrapper.InstanceData) : GroupType(), TimeChild {

        override val instanceDatas = listOf(instanceData)

        override val firstInstanceData = instanceData

        override fun toContentDelegate(
            groupAdapter: GroupListFragment.GroupAdapter,
            indentation: Int,
            nodeCollection: NodeCollection,
        ) = NotDoneNode.ContentDelegate.Instance(groupAdapter, instanceData, indentation)
    }
}