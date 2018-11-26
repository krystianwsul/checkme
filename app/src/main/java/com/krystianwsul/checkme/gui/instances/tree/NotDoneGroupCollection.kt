package com.krystianwsul.checkme.gui.instances.tree

import com.krystianwsul.checkme.utils.InstanceKey
import com.krystianwsul.checkme.utils.time.TimeStamp
import com.krystianwsul.treeadapter.NodeContainer
import com.krystianwsul.treeadapter.TreeNode
import com.krystianwsul.treeadapter.TreeViewAdapter


class NotDoneGroupCollection(private val indentation: Int, val nodeCollection: NodeCollection, private val nodeContainer: NodeContainer, private val selectable: Boolean) {

    private val notDoneGroupNodes = mutableListOf<NotDoneGroupNode>()

    val expandedGroups get() = notDoneGroupNodes.filter { !it.singleInstance() && it.expanded() }.map { it.exactTimeStamp.toTimeStamp() }

    fun initialize(notDoneInstanceDatas: List<GroupListFragment.InstanceData>, expandedGroups: List<TimeStamp>?, expandedInstances: Map<InstanceKey, Boolean>?, selectedNodes: List<InstanceKey>?) = if (nodeCollection.useGroups) {
        notDoneInstanceDatas.groupBy { it.InstanceTimeStamp }
                .values
                .map { newNotDoneGroupNode(this, it.toMutableList(), expandedGroups, expandedInstances, selectedNodes) }
    } else {
        notDoneInstanceDatas.map { newNotDoneGroupNode(this, mutableListOf(it), expandedGroups, expandedInstances, selectedNodes) }
    }

    fun remove(notDoneGroupNode: NotDoneGroupNode, x: TreeViewAdapter.Placeholder) {
        check(notDoneGroupNodes.contains(notDoneGroupNode))

        notDoneGroupNodes.remove(notDoneGroupNode)
        nodeContainer.remove(notDoneGroupNode.treeNode, x)
    }

    fun add(instanceData: GroupListFragment.InstanceData, x: TreeViewAdapter.Placeholder) {
        val exactTimeStamp = instanceData.InstanceTimeStamp.toExactTimeStamp()

        notDoneGroupNodes.filter { it.exactTimeStamp == exactTimeStamp }.let {
            if (it.isEmpty() || !nodeCollection.useGroups) {
                nodeCollection.nodeContainer.add(newNotDoneGroupNode(this, mutableListOf(instanceData), null, null, null), x)
            } else {
                it.single().addInstanceData(instanceData, x)
            }
        }
    }

    private fun newNotDoneGroupNode(notDoneGroupCollection: NotDoneGroupCollection, instanceDatas: MutableList<GroupListFragment.InstanceData>, expandedGroups: List<TimeStamp>?, expandedInstances: Map<InstanceKey, Boolean>?, selectedNodes: List<InstanceKey>?): TreeNode {
        check(!instanceDatas.isEmpty())

        val notDoneGroupNode = NotDoneGroupNode(indentation, notDoneGroupCollection, instanceDatas, selectable)

        val notDoneGroupTreeNode = notDoneGroupNode.initialize(expandedGroups, expandedInstances, selectedNodes, nodeContainer)

        notDoneGroupNodes.add(notDoneGroupNode)

        return notDoneGroupTreeNode
    }

    fun addExpandedInstances(expandedInstances: Map<InstanceKey, Boolean>) {
        for (notDoneGroupNode in notDoneGroupNodes)
            notDoneGroupNode.addExpandedInstances(expandedInstances)
    }
}
