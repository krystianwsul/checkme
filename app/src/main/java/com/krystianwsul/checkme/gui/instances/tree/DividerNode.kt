package com.krystianwsul.checkme.gui.instances.tree

import android.graphics.Color
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.view.View
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.utils.InstanceKey
import com.krystianwsul.treeadapter.ModelNode
import com.krystianwsul.treeadapter.NodeContainer
import com.krystianwsul.treeadapter.TreeNode
import java.util.*

class DividerNode(density: Float, indentation: Int, val nodeCollection: NodeCollection) : GroupHolderNode(density, indentation), ModelNode {

    private lateinit var treeNode: TreeNode

    private val doneInstanceNodes = ArrayList<DoneInstanceNode>()

    val groupAdapter get() = nodeCollection.groupAdapter

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

        val doneInstanceNode = DoneInstanceNode(density, indentation, instanceData, this)

        val childTreeNode = doneInstanceNode.initialize(treeNode, expandedInstances)

        doneInstanceNodes.add(doneInstanceNode)

        return childTreeNode
    }

    fun expanded() = treeNode.isExpanded

    fun addExpandedInstances(expandedInstances: Map<InstanceKey, Boolean>) {
        for (doneInstanceNode in doneInstanceNodes)
            doneInstanceNode.addExpandedInstances(expandedInstances)
    }

    override val name get() = Triple(groupListFragment.getString(R.string.done), ContextCompat.getColor(groupListFragment.activity!!, R.color.textPrimary), true)

    override val expand get() = Pair(if (treeNode.isExpanded) R.drawable.ic_expand_less_black_36dp else R.drawable.ic_expand_more_black_36dp, treeNode.expandListener)

    override val checkBoxVisibility = View.INVISIBLE

    override val checkBoxChecked get() = throw UnsupportedOperationException()

    override val checkBoxOnClickListener get() = throw UnsupportedOperationException()

    override val separatorVisibility get() = if (treeNode.separatorVisibility) View.VISIBLE else View.INVISIBLE

    override val backgroundColor = Color.TRANSPARENT

    override fun getOnLongClickListener(viewHolder: RecyclerView.ViewHolder) = treeNode.onLongClickListener

    override val onClickListener get() = treeNode.onClickListener

    fun remove(doneInstanceNode: DoneInstanceNode) {
        check(doneInstanceNodes.contains(doneInstanceNode))
        doneInstanceNodes.remove(doneInstanceNode)

        treeNode.remove(doneInstanceNode.treeNode)
    }

    fun add(instanceData: GroupListFragment.InstanceData) {
        treeNode.add(newChildTreeNode(instanceData, null))
    }

    override val isSelectable = false

    override fun onClick() = Unit

    override fun compareTo(other: ModelNode): Int {
        check(other is NoteNode || other is NotDoneGroupNode || other is UnscheduledNode)
        return 1
    }

    override val isVisibleWhenEmpty = false

    override val isVisibleDuringActionMode = false

    override val isSeparatorVisibleWhenNotExpanded = false
}
