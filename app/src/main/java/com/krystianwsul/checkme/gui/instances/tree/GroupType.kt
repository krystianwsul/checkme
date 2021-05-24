package com.krystianwsul.checkme.gui.instances.tree

import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.checkme.gui.instances.list.GroupListFragment
import com.krystianwsul.common.time.TimeStamp
import com.krystianwsul.common.utils.ProjectKey

sealed class GroupType {

    companion object {

        fun getContentDelegates(
            instanceDatas: List<GroupListDataWrapper.InstanceData>,
            groupingMode: NodeCollection.GroupingMode,
            groupAdapter: GroupListFragment.GroupAdapter,
            indentation: Int,
            nodeCollection: NodeCollection,
        ): List<NotDoneNode.ContentDelegate> {
            if (instanceDatas.isEmpty()) return emptyList()

            val groupTypes: List<GroupType> = when (groupingMode) {
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
                                TimeProject(timeStamp, projectKey, instanceDatas)
                            } ?: Time(timeStamp, instanceDatas)
                        } else {
                            // if there's just one, there's our node
                            Single(instanceDatas.single())
                        }
                    }
                }
                NodeCollection.GroupingMode.PROJECT -> {
                    // we'll potentially group these by project

                    val projectGroups = instanceDatas.groupBy { it.projectInfo?.projectDetails?.projectKey }

                    val groupTypesForShared = projectGroups.filterKeys { it != null }.map { (projectKey, instanceDatas) ->
                        check(instanceDatas.isNotEmpty())

                        if (instanceDatas.size > 1) {
                            Project(projectKey!!, instanceDatas)
                        } else {
                            Single(instanceDatas.single())
                        }
                    }

                    val groupTypesForPrivate = projectGroups[null]?.map(GroupType::Single).orEmpty()

                    listOf(groupTypesForShared, groupTypesForPrivate).flatten()
                }
                NodeCollection.GroupingMode.NONE -> instanceDatas.map(GroupType::Single)
            }

            return groupTypes.map { it.toContentDelegate(groupAdapter, indentation, nodeCollection) }
        }
    }

    abstract fun toContentDelegate(
        groupAdapter: GroupListFragment.GroupAdapter,
        indentation: Int,
        nodeCollection: NodeCollection,
    ): NotDoneNode.ContentDelegate

    data class Time(
        val timeStamp: TimeStamp,
        override val instanceDatas: List<GroupListDataWrapper.InstanceData>,
    ) : GroupType(), Multi {

        override fun toContentDelegate(
            groupAdapter: GroupListFragment.GroupAdapter,
            indentation: Int,
            nodeCollection: NodeCollection,
        ) = NotDoneNode.ContentDelegate.Group(
            groupAdapter,
            instanceDatas,
            indentation,
            NodeCollection.GroupingMode.PROJECT,
            nodeCollection,
        )
    }

    data class TimeProject(
        val timeStamp: TimeStamp,
        val projectKey: ProjectKey.Shared,
        override val instanceDatas: List<GroupListDataWrapper.InstanceData>,
    ) : GroupType(), Multi {

        override fun toContentDelegate(
            groupAdapter: GroupListFragment.GroupAdapter,
            indentation: Int,
            nodeCollection: NodeCollection,
        ) = NotDoneNode.ContentDelegate.Group(
            groupAdapter,
            instanceDatas,
            indentation,
            NodeCollection.GroupingMode.NONE,
            nodeCollection,
        ) // todo project new delegate
    }

    data class Project(
        val projectKey: ProjectKey.Shared,
        override val instanceDatas: List<GroupListDataWrapper.InstanceData>,
    ) : GroupType(), Multi {

        override fun toContentDelegate(
            groupAdapter: GroupListFragment.GroupAdapter,
            indentation: Int,
            nodeCollection: NodeCollection,
        ) = NotDoneNode.ContentDelegate.Group(
            groupAdapter,
            instanceDatas,
            indentation,
            NodeCollection.GroupingMode.NONE,
            nodeCollection,
        ) // todo project new delegate
    }

    sealed interface Multi {

        val instanceDatas: List<GroupListDataWrapper.InstanceData>
    }

    data class Single(val instanceData: GroupListDataWrapper.InstanceData) : GroupType() {

        override fun toContentDelegate(
            groupAdapter: GroupListFragment.GroupAdapter,
            indentation: Int,
            nodeCollection: NodeCollection,
        ) = NotDoneNode.ContentDelegate.Instance(groupAdapter, instanceData, indentation)
    }
}