package com.krystianwsul.checkme.gui.instances.tree

import com.krystianwsul.checkme.utils.InstanceKey
import com.krystianwsul.checkme.utils.time.TimeStamp
import com.krystianwsul.treeadapter.NodeContainer
import com.krystianwsul.treeadapter.TreeNode


class NotDoneGroupCollection(private val density: Float, private val indentation: Int, val nodeCollection: NodeCollection, private val nodeContainer: NodeContainer, private val selectable: Boolean) {

    private val notDoneGroupNodes = mutableListOf<NotDoneGroupNode>()

    val expandedGroups get() = notDoneGroupNodes.filter { !it.singleInstance() && it.expanded() }.map { it.exactTimeStamp.toTimeStamp() }

    fun initialize(notDoneInstanceDatas: List<GroupListFragment.InstanceData>, expandedGroups: List<TimeStamp>?, expandedInstances: Map<InstanceKey, Boolean>?, selectedNodes: List<InstanceKey>?) = if (nodeCollection.useGroups) {
        notDoneInstanceDatas.groupBy { it.InstanceTimeStamp }
                .values
                .map { newNotDoneGroupNode(this, it.toMutableList(), expandedGroups, expandedInstances, selectedNodes) }
    } else {
        notDoneInstanceDatas.map { newNotDoneGroupNode(this, mutableListOf(it), expandedGroups, expandedInstances, selectedNodes) }
    }

    fun remove(notDoneGroupNode: NotDoneGroupNode) {
        check(notDoneGroupNodes.contains(notDoneGroupNode))

        notDoneGroupNodes.remove(notDoneGroupNode)
        nodeContainer.remove(notDoneGroupNode.treeNode)
    }

    fun add(instanceData: GroupListFragment.InstanceData) {
        val exactTimeStamp = instanceData.InstanceTimeStamp.toExactTimeStamp()

        notDoneGroupNodes.filter { it.exactTimeStamp == exactTimeStamp }.let {
            if (it.isEmpty() || !nodeCollection.useGroups) {
                nodeCollection.nodeContainer.add(newNotDoneGroupNode(this, mutableListOf(instanceData), null, null, null))
            } else {
                it.single().addInstanceData(instanceData)
            }
        }
    }

    private fun newNotDoneGroupNode(notDoneGroupCollection: NotDoneGroupCollection, instanceDatas: MutableList<GroupListFragment.InstanceData>, expandedGroups: List<TimeStamp>?, expandedInstances: Map<InstanceKey, Boolean>?, selectedNodes: List<InstanceKey>?): TreeNode {
        check(!instanceDatas.isEmpty())

        val notDoneGroupNode = NotDoneGroupNode(density, indentation, notDoneGroupCollection, instanceDatas, selectable)

        val notDoneGroupTreeNode = notDoneGroupNode.initialize(expandedGroups, expandedInstances, selectedNodes, nodeContainer)

        notDoneGroupNodes.add(notDoneGroupNode)

        return notDoneGroupTreeNode
    }

    fun addExpandedInstances(expandedInstances: Map<InstanceKey, Boolean>) {
        for (notDoneGroupNode in notDoneGroupNodes)
            notDoneGroupNode.addExpandedInstances(expandedInstances)
    }
}
