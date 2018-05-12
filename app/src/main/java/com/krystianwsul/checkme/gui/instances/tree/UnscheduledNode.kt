package com.krystianwsul.checkme.gui.instances.tree

import android.graphics.Color
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.view.View
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.utils.TaskKey
import com.krystianwsul.treeadapter.ModelNode
import com.krystianwsul.treeadapter.NodeContainer
import com.krystianwsul.treeadapter.TreeNode

class UnscheduledNode(density: Float, private val nodeCollection: NodeCollection) : GroupHolderNode(density, 0), ModelNode, TaskParent {

    private lateinit var treeNode: TreeNode

    private val taskNodes = mutableListOf<TaskNode>()

    val expandedTaskKeys get() = taskNodes.flatMap { it.expandedTaskKeys }

    private val groupListFragment by lazy { groupAdapter.mGroupListFragment }

    fun initialize(expanded: Boolean, nodeContainer: NodeContainer, taskDatas: List<GroupListFragment.TaskData>, expandedTaskKeys: List<TaskKey>?): TreeNode {
        check(!expanded || !taskDatas.isEmpty())

        treeNode = TreeNode(this, nodeContainer, expanded, false)

        treeNode.setChildTreeNodes(taskDatas.map { newChildTreeNode(it, expandedTaskKeys) })

        return treeNode
    }

    private fun newChildTreeNode(taskData: GroupListFragment.TaskData, expandedTaskKeys: List<TaskKey>?) = TaskNode(mDensity, 0, taskData, this).let {
        taskNodes.add(it)

        it.initialize(treeNode, expandedTaskKeys)
    }

    fun expanded() = treeNode.isExpanded

    override val groupAdapter by lazy { nodeCollection.groupAdapter }

    override fun compareTo(other: ModelNode) = if (other is DividerNode) {
        -1
    } else {
        check(other is NotDoneGroupNode)

        1
    }

    override fun getName() = Triple(groupListFragment.getString(R.string.noReminder), ContextCompat.getColor(groupListFragment.activity!!, R.color.textPrimary), true)

    override fun getExpand() = Pair(if (treeNode.isExpanded) R.drawable.ic_expand_less_black_36dp else R.drawable.ic_expand_more_black_36dp, treeNode.expandListener)

    override fun getCheckBoxVisibility() = View.INVISIBLE

    override fun getCheckBoxChecked() = throw UnsupportedOperationException()

    override fun getCheckBoxOnClickListener() = throw UnsupportedOperationException()

    override fun getSeparatorVisibility() = if (this.treeNode.separatorVisibility) View.VISIBLE else View.INVISIBLE

    override fun getBackgroundColor() = Color.TRANSPARENT

    override fun getOnLongClickListener(viewHolder: RecyclerView.ViewHolder) = treeNode.onLongClickListener

    override fun getOnClickListener() = treeNode.onClickListener

    override val isSelectable = false

    override fun onClick() = Unit

    override val isVisibleWhenEmpty = false

    override val isVisibleDuringActionMode = false

    override val isSeparatorVisibleWhenNotExpanded = false
}
