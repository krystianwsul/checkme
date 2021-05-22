package com.krystianwsul.checkme.gui.instances.tree

import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.checkme.gui.instances.list.GroupListFragment
import com.krystianwsul.common.time.TimeStamp
import com.krystianwsul.common.utils.ProjectKey

sealed class GroupType {

    companion object {

        fun getContentDelegates(
            notDoneInstanceDatas: List<GroupListDataWrapper.InstanceData>,
            groupingMode: NodeCollection.GroupingMode,
            groupAdapter: GroupListFragment.GroupAdapter,
            indentation: Int,
        ): List<NotDoneNode.ContentDelegate> {
            if (notDoneInstanceDatas.isEmpty()) return emptyList()

            val groupTypes: List<GroupType> = when (groupingMode) {
                NodeCollection.GroupingMode.TIME -> {
                    val timeGroups = notDoneInstanceDatas.groupBy { it.instanceTimeStamp }

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
                            None(instanceDatas.single())
                        }
                    }
                }
                NodeCollection.GroupingMode.PROJECT -> {
                    // we'll potentially group these by project

                    val projectGroups = notDoneInstanceDatas.groupBy { it.projectInfo?.projectDetails?.projectKey }

                    val groupTypesForShared = projectGroups.filterKeys { it != null }.map { (projectKey, instanceDatas) ->
                        check(instanceDatas.isNotEmpty())

                        if (instanceDatas.size > 1) {
                            Project(projectKey!!, instanceDatas)
                        } else {
                            None(instanceDatas.single())
                        }
                    }

                    val groupTypesForPrivate = projectGroups[null]?.map(GroupType::None).orEmpty()

                    listOf(groupTypesForShared, groupTypesForPrivate).flatten()
                }
                NodeCollection.GroupingMode.NONE -> notDoneInstanceDatas.map(GroupType::None)
            }

            return groupTypes.map { it.toContentDelegate(groupAdapter, indentation) }
        }
    }

    abstract fun toContentDelegate(
        groupAdapter: GroupListFragment.GroupAdapter,
        indentation: Int,
    ): NotDoneNode.ContentDelegate

    data class Time(val timeStamp: TimeStamp, val instanceDatas: List<GroupListDataWrapper.InstanceData>) : GroupType() {

        override fun toContentDelegate(
            groupAdapter: GroupListFragment.GroupAdapter,
            indentation: Int
        ) = NotDoneNode.ContentDelegate.Group(groupAdapter, instanceDatas, indentation)
    }

    data class TimeProject(
        val timeStamp: TimeStamp,
        val projectKey: ProjectKey.Shared,
        val instanceDatas: List<GroupListDataWrapper.InstanceData>,
    ) : GroupType() {

        override fun toContentDelegate(
            groupAdapter: GroupListFragment.GroupAdapter,
            indentation: Int
        ) = NotDoneNode.ContentDelegate.Group(groupAdapter, instanceDatas, indentation) // todo project new delegate
    }

    data class Project(
        val projectKey: ProjectKey.Shared,
        val instanceDatas: List<GroupListDataWrapper.InstanceData>,
    ) : GroupType() {

        override fun toContentDelegate(
            groupAdapter: GroupListFragment.GroupAdapter,
            indentation: Int
        ) = NotDoneNode.ContentDelegate.Group(groupAdapter, instanceDatas, indentation) // todo project new delegate
    }

    data class None(val instanceData: GroupListDataWrapper.InstanceData) : GroupType() {

        override fun toContentDelegate(
            groupAdapter: GroupListFragment.GroupAdapter,
            indentation: Int
        ) = NotDoneNode.ContentDelegate.Instance(groupAdapter, instanceData, indentation)
    }
}