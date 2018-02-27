package com.krystianwsul.checkme.gui.instances.tree

import android.graphics.Color
import android.support.v4.content.ContextCompat
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

    private val groupListFragment = groupAdapter.mGroupListFragment

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

    fun expanded() = treeNode.expanded()

    override fun getGroupAdapter() = nodeCollection.groupAdapter

    override fun compareTo(other: ModelNode) = if (other is DividerNode) {
        -1
    } else {
        check(other is NotDoneGroupNode)

        1
    }

    override fun getNameVisibility() = View.VISIBLE

    override fun getName() = groupListFragment.getString(R.string.noReminder)

    override fun getNameColor() = ContextCompat.getColor(groupListFragment.activity!!, R.color.textPrimary)

    override fun getNameSingleLine() = true

    override fun getDetailsVisibility() = View.GONE

    override fun getDetails() = throw UnsupportedOperationException()

    override fun getDetailsColor() = throw UnsupportedOperationException()

    override fun getChildren(): Nothing? = null

    override fun getExpandVisibility(): Int {
        check(treeNode.expandVisible)

        return View.VISIBLE
    }

    override fun getExpandImageResource(): Int {
        check(treeNode.expandVisible)

        return if (treeNode.expanded())
            R.drawable.ic_expand_less_black_36dp
        else
            R.drawable.ic_expand_more_black_36dp
    }

    override fun getExpandOnClickListener(): View.OnClickListener {
        check(treeNode.expandVisible)

        return treeNode.expandListener
    }

    override fun getCheckBoxVisibility() = View.INVISIBLE

    override fun getCheckBoxChecked() = throw UnsupportedOperationException()

    override fun getCheckBoxOnClickListener() = throw UnsupportedOperationException()

    override fun getSeparatorVisibility() = if (this.treeNode.separatorVisibility) View.VISIBLE else View.INVISIBLE

    override fun getBackgroundColor() = Color.TRANSPARENT

    override fun getOnLongClickListener() = treeNode.onLongClickListener

    override fun getOnClickListener() = treeNode.onClickListener

    override fun selectable() = false

    override fun onClick() = Unit

    override fun visibleWhenEmpty() = false

    override fun visibleDuringActionMode() = false

    override fun separatorVisibleWhenNotExpanded() = false
}
