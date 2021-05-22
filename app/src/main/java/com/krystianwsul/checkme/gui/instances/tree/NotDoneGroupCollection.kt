package com.krystianwsul.checkme.gui.instances.tree

import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.checkme.gui.instances.list.GroupListFragment
import com.krystianwsul.checkme.gui.tree.AbstractHolder
import com.krystianwsul.checkme.gui.utils.flatten
import com.krystianwsul.common.time.TimeStamp
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.treeadapter.NodeContainer
import com.krystianwsul.treeadapter.TreeNode


class NotDoneGroupCollection(
    private val indentation: Int,
    private val nodeCollection: NodeCollection,
    private val nodeContainer: NodeContainer<AbstractHolder>,
) {

    private val notDoneGroupNodes = mutableListOf<NotDoneGroupNode>()

    val groupExpansionStates
        get() = notDoneGroupNodes.map { it.contentDelegate }
            .filterIsInstance<NotDoneNode.ContentDelegate.Group>()
            .map { it.timeStamp to it.treeNode.expansionState }
            .toMap()

    private sealed class GroupType {

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

    fun initialize(
        notDoneInstanceDatas: List<GroupListDataWrapper.InstanceData>,
        collectionState: CollectionState,
    ): List<TreeNode<AbstractHolder>> {
        val groupTypes: List<GroupType> = if (notDoneInstanceDatas.isEmpty()) {
            listOf()
        } else {
            when (nodeCollection.groupingMode) {
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
                                GroupType.TimeProject(timeStamp, projectKey, instanceDatas)
                            } ?: GroupType.Time(timeStamp, instanceDatas)
                        } else {
                            // if there's just one, there's our node
                            GroupType.None(instanceDatas.single())
                        }
                    }
                }
                NodeCollection.GroupingMode.PROJECT -> {
                    // we'll potentially group these by project

                    val projectGroups = notDoneInstanceDatas.groupBy { it.projectInfo?.projectDetails?.projectKey }

                    val groupTypesForShared = projectGroups.filterKeys { it != null }.map { (projectKey, instanceDatas) ->
                        check(instanceDatas.isNotEmpty())

                        if (instanceDatas.size > 1) {
                            GroupType.Project(projectKey!!, instanceDatas)
                        } else {
                            GroupType.None(instanceDatas.single())
                        }
                    }

                    val groupTypesForPrivate = projectGroups[null]?.map(GroupType::None).orEmpty()

                    listOf(groupTypesForShared, groupTypesForPrivate).flatten()
                }
                NodeCollection.GroupingMode.NONE -> notDoneInstanceDatas.map(GroupType::None)
            }
        }

        val contentDelegates = groupTypes.map { it.toContentDelegate(nodeCollection.groupAdapter, indentation) }

        val nodePairs = contentDelegates.map {
            val notDoneGroupNode = NotDoneGroupNode(indentation, nodeCollection, it)

            val notDoneGroupTreeNode = notDoneGroupNode.initialize(collectionState, nodeContainer)

            notDoneGroupTreeNode to notDoneGroupNode
        }

        notDoneGroupNodes += nodePairs.map { it.second }

        return nodePairs.map { it.first }
    }

    val instanceExpansionStates get() = notDoneGroupNodes.map { it.instanceExpansionStates }.flatten()
}
