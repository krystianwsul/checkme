package com.krystianwsul.checkme.gui.instances.tree

import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.checkme.gui.tree.BaseHolder
import com.krystianwsul.common.time.TimeStamp
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.treeadapter.NodeContainer
import com.krystianwsul.treeadapter.TreeNode
import com.krystianwsul.treeadapter.TreeViewAdapter


class NotDoneGroupCollection(
        private val indentation: Int,
        val nodeCollection: NodeCollection,
        private val nodeContainer: NodeContainer<BaseHolder>,
) {

    private val notDoneGroupNodes = mutableListOf<NotDoneGroupNode>()

    val expandedGroups
        get() = notDoneGroupNodes.filter {
            !it.singleInstance() && it.expanded()
        }.map { it.exactTimeStamp.toTimeStamp() }

    fun initialize(
            notDoneInstanceDatas: List<GroupListDataWrapper.InstanceData>,
            expandedGroups: List<TimeStamp>,
            expandedInstances: Map<InstanceKey, Boolean>,
            selectedInstances: List<InstanceKey>,
            selectedGroups: List<Long>,
    ) = if (nodeCollection.useGroups) {
        notDoneInstanceDatas.groupBy { it.instanceTimeStamp }
                .values
                .map {
                    newNotDoneGroupNode(
                            this,
                            it.toMutableList(),
                            expandedGroups,
                            expandedInstances,
                            selectedInstances,
                            selectedGroups
                    )
                }
    } else {
        notDoneInstanceDatas.map {
            newNotDoneGroupNode(
                    this,
                    mutableListOf(it),
                    expandedGroups,
                    expandedInstances,
                    selectedInstances,
                    selectedGroups
            )
        }
    }

    fun remove(notDoneGroupNode: NotDoneGroupNode, x: TreeViewAdapter.Placeholder) {
        check(notDoneGroupNodes.contains(notDoneGroupNode))

        notDoneGroupNodes.remove(notDoneGroupNode)
        nodeContainer.remove(notDoneGroupNode.treeNode, x)
    }

    fun add(instanceData: GroupListDataWrapper.InstanceData, x: TreeViewAdapter.Placeholder) {
        val exactTimeStamp = instanceData.instanceTimeStamp.toLocalExactTimeStamp()

        notDoneGroupNodes.filter { it.exactTimeStamp == exactTimeStamp }.let {
            if (it.isEmpty() || !nodeCollection.useGroups) {
                nodeCollection.nodeContainer.add(newNotDoneGroupNode(this, mutableListOf(instanceData), listOf(), mapOf(), listOf(), listOf()), x)
            } else {
                it.single().addInstanceData(instanceData, x)
            }
        }
    }

    private fun newNotDoneGroupNode(
            notDoneGroupCollection: NotDoneGroupCollection,
            instanceDatas: MutableList<GroupListDataWrapper.InstanceData>,
            expandedGroups: List<TimeStamp>,
            expandedInstances: Map<InstanceKey, Boolean>,
            selectedInstances: List<InstanceKey>,
            selectedGroups: List<Long>,
    ): TreeNode<BaseHolder> {
        check(instanceDatas.isNotEmpty())

        val notDoneGroupNode = NotDoneGroupNode(
                indentation,
                notDoneGroupCollection,
                instanceDatas,
                nodeCollection.searchResults,
                nodeCollection.parentNode
        )

        val notDoneGroupTreeNode = notDoneGroupNode.initialize(
                expandedGroups,
                expandedInstances,
                selectedInstances,
                selectedGroups,
                nodeContainer
        )

        notDoneGroupNodes.add(notDoneGroupNode)

        return notDoneGroupTreeNode
    }

    fun addExpandedInstances(expandedInstances: MutableMap<InstanceKey, Boolean>) {
        for (notDoneGroupNode in notDoneGroupNodes)
            notDoneGroupNode.addExpandedInstances(expandedInstances)
    }
}
