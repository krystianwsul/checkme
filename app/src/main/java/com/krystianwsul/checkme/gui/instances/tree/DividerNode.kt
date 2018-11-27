package com.krystianwsul.checkme.gui.instances.tree

import android.support.v7.widget.RecyclerView
import android.view.View
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.utils.InstanceKey
import com.krystianwsul.treeadapter.*
import java.util.*

class DividerNode(indentation: Int, val nodeCollection: NodeCollection) : GroupHolderNode(indentation) {

    override lateinit var treeNode: TreeNode
        private set

    private val doneInstanceNodes = ArrayList<DoneInstanceNode>()

    private val groupAdapter get() = nodeCollection.groupAdapter

    private val groupListFragment get() = groupAdapter.mGroupListFragment

    fun initialize(expanded: Boolean, nodeContainer: NodeContainer, doneInstanceDatas: List<GroupListFragment.InstanceData>, expandedInstances: Map<InstanceKey, Boolean>?): TreeNode {
        check(!expanded || !doneInstanceDatas.isEmpty())

        treeNode = TreeNode(this, nodeContainer, expanded, false)

        val childTreeNodes = doneInstanceDatas.map { newChildTreeNode(it, expandedInstances) }

        treeNode.setChildTreeNodes(childTreeNodes)

        return treeNode
    }

    private fun newChildTreeNode(instanceData: GroupListFragment.InstanceData, expandedInstances: Map<InstanceKey, Boolean>?): TreeNode {
        checkNotNull(instanceData.Done)

        val doneInstanceNode = DoneInstanceNode(indentation, instanceData, this)

        val childTreeNode = doneInstanceNode.initialize(treeNode, expandedInstances)

        doneInstanceNodes.add(doneInstanceNode)

        return childTreeNode
    }

    fun expanded() = treeNode.isExpanded

    fun addExpandedInstances(expandedInstances: Map<InstanceKey, Boolean>) {
        for (doneInstanceNode in doneInstanceNodes)
            doneInstanceNode.addExpandedInstances(expandedInstances)
    }

    override val name get() = Triple(groupListFragment.activity.getString(R.string.done), colorPrimary, true)

    override val expand get() = Pair(treeNode.isExpanded, treeNode.expandListener)

    override val checkBoxVisibility = View.INVISIBLE

    override fun getOnLongClickListener(viewHolder: RecyclerView.ViewHolder) = treeNode.onLongClickListener

    override val onClickListener get() = treeNode.onClickListener

    fun remove(doneInstanceNode: DoneInstanceNode, x: TreeViewAdapter.Placeholder) {
        check(doneInstanceNodes.contains(doneInstanceNode))
        doneInstanceNodes.remove(doneInstanceNode)

        treeNode.remove(doneInstanceNode.treeNode, x)
    }

    fun add(instanceData: GroupListFragment.InstanceData, x: TreeViewAdapter.Placeholder) = treeNode.add(newChildTreeNode(instanceData, null), x)

    override val isSelectable = false

    override fun onClick() = Unit

    override fun compareTo(other: ModelNode): Int {
        check(other is NoteNode || other is NotDoneGroupNode || other is UnscheduledNode)
        return 1
    }

    override val isVisibleWhenEmpty = false

    override val isVisibleDuringActionMode = false

    override val isSeparatorVisibleWhenNotExpanded = false

    override val state = State(nodeCollection.nodeContainer.id)

    data class State(val id: Any) : ModelState {

        override fun same(other: ModelState) = (other as? State)?.id == id
    }
}
