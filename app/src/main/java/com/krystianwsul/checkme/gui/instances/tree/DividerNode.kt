package com.krystianwsul.checkme.gui.instances.tree

import android.view.View
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.utils.InstanceKey
import com.krystianwsul.treeadapter.ModelNode
import com.krystianwsul.treeadapter.NodeContainer
import com.krystianwsul.treeadapter.TreeNode
import com.krystianwsul.treeadapter.TreeViewAdapter
import java.util.*

class DividerNode(indentation: Int, val nodeCollection: NodeCollection) : GroupHolderNode(indentation) {

    override val id get() = Id(nodeCollection.nodeContainer.id)

    data class Id(val id: Any)

    override lateinit var treeNode: TreeNode
        private set

    private val doneInstanceNodes = ArrayList<DoneInstanceNode>()

    private val groupAdapter get() = nodeCollection.groupAdapter

    private val groupListFragment get() = groupAdapter.groupListFragment

    fun initialize(expanded: Boolean, nodeContainer: NodeContainer, doneInstanceDatas: List<GroupListFragment.InstanceData>, expandedInstances: Map<InstanceKey, Boolean>, selectedInstances: List<InstanceKey>): TreeNode {
        check(!expanded || doneInstanceDatas.isNotEmpty())

        treeNode = TreeNode(this, nodeContainer, expanded, false)

        val childTreeNodes = doneInstanceDatas.map { newChildTreeNode(it, expandedInstances, selectedInstances) }

        treeNode.setChildTreeNodes(childTreeNodes)

        return treeNode
    }

    private fun newChildTreeNode(instanceData: GroupListFragment.InstanceData, expandedInstances: Map<InstanceKey, Boolean>, selectedInstances: List<InstanceKey>): TreeNode {
        checkNotNull(instanceData.done)

        val doneInstanceNode = DoneInstanceNode(indentation, instanceData, this)

        val childTreeNode = doneInstanceNode.initialize(treeNode, expandedInstances, selectedInstances)

        doneInstanceNodes.add(doneInstanceNode)

        return childTreeNode
    }

    fun expanded() = treeNode.isExpanded

    fun addExpandedInstances(expandedInstances: MutableMap<InstanceKey, Boolean>) {
        for (doneInstanceNode in doneInstanceNodes)
            doneInstanceNode.addExpandedInstances(expandedInstances)
    }

    override val name get() = NameData(groupListFragment.activity.getString(R.string.done))

    override val checkBoxVisibility = View.INVISIBLE

    fun remove(doneInstanceNode: DoneInstanceNode, x: TreeViewAdapter.Placeholder) {
        check(doneInstanceNodes.contains(doneInstanceNode))
        doneInstanceNodes.remove(doneInstanceNode)

        treeNode.remove(doneInstanceNode.treeNode, x)
    }

    fun add(instanceData: GroupListFragment.InstanceData, x: TreeViewAdapter.Placeholder) = treeNode.add(newChildTreeNode(instanceData, mapOf(), listOf()), x)

    override fun compareTo(other: ModelNode): Int {
        check(other is NoteNode || other is NotDoneGroupNode || other is UnscheduledNode || other is ImageNode)
        return 1
    }

    override val isVisibleWhenEmpty = false
}
