package com.krystianwsul.checkme.gui.instances.tree

import android.graphics.Color
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.view.View

import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.gui.tasks.ShowTaskActivity
import com.krystianwsul.checkme.utils.TaskKey
import com.krystianwsul.treeadapter.ModelNode
import com.krystianwsul.treeadapter.TreeNode

import junit.framework.Assert

class TaskNode(density: Float, indentation: Int, private val taskData: GroupListFragment.TaskData, private val taskParent: TaskParent) : GroupHolderNode(density, indentation), ModelNode, TaskParent {

    private lateinit var treeNode: TreeNode

    private val taskNodes = mutableListOf<TaskNode>()

    private val groupListFragment by lazy { groupAdapter.mGroupListFragment }

    val expandedTaskKeys: List<TaskKey>
        get() = if (taskNodes.isEmpty()) {
            Assert.assertTrue(!expanded())

            listOf()
        } else {
            mutableListOf<TaskKey>().apply {
                if (expanded())
                    add(taskData.mTaskKey)

                addAll(taskNodes.flatMap { it.expandedTaskKeys })
            }
        }

    fun initialize(parentTreeNode: TreeNode, expandedTaskKeys: List<TaskKey>?): TreeNode {
        val expanded = expandedTaskKeys?.contains(taskData.mTaskKey) == true && !taskData.Children.isEmpty()

        treeNode = TreeNode(this, parentTreeNode, expanded, false)

        treeNode.setChildTreeNodes(taskData.Children.map { newChildTreeNode(it, expandedTaskKeys) })

        return treeNode
    }

    private fun newChildTreeNode(taskData: GroupListFragment.TaskData, expandedTaskKeys: List<TaskKey>?) = TaskNode(density, indentation + 1, taskData, this).let {
        taskNodes.add(it)

        it.initialize(treeNode, expandedTaskKeys)
    }

    override val groupAdapter by lazy { taskParent.groupAdapter }

    private fun expanded() = treeNode.isExpanded

    override fun compareTo(other: ModelNode) = (other as TaskNode).taskData.mStartExactTimeStamp.let {
        if (indentation == 0) {
            -taskData.mStartExactTimeStamp.compareTo(it)
        } else {
            taskData.mStartExactTimeStamp.compareTo(it)
        }
    }

    override val name get() = Triple(taskData.Name, ContextCompat.getColor(groupListFragment.activity!!, R.color.textPrimary), true)

    override val children
        get() = if ((taskData.Children.isEmpty() || expanded()) && taskData.mNote.isNullOrEmpty()) {
        null
    } else {
        val text = if (!expanded() && !taskData.Children.isEmpty()) {
            taskData.Children
                    .sortedBy { it.mStartExactTimeStamp }
                    .joinToString(", ") { it.Name }
        } else {
            Assert.assertTrue(!taskData.mNote.isNullOrEmpty())

            taskData.mNote!!
        }

        val color = ContextCompat.getColor(groupListFragment.activity!!, R.color.textSecondary)

        Pair(text, color)
    }

    override val expand
        get(): Pair<Int, View.OnClickListener>? {
        return if (taskData.Children.isEmpty()) {
            null
        } else {
            Pair(if (treeNode.isExpanded) R.drawable.ic_expand_less_black_36dp else R.drawable.ic_expand_more_black_36dp, treeNode.expandListener)
        }
    }

    override val checkBoxVisibility = View.INVISIBLE

    override val checkBoxChecked get() = throw UnsupportedOperationException()

    override val checkBoxOnClickListener get() = throw UnsupportedOperationException()

    override val separatorVisibility get() = if (this.treeNode.separatorVisibility) View.VISIBLE else View.INVISIBLE

    override val backgroundColor = Color.TRANSPARENT

    override fun getOnLongClickListener(viewHolder: RecyclerView.ViewHolder) = treeNode.onLongClickListener

    override val onClickListener get() = treeNode.onClickListener

    override val isSelectable = false

    override fun onClick() {
        groupListFragment.activity!!.startActivity(ShowTaskActivity.newIntent(taskData.mTaskKey))
    }

    override val isVisibleWhenEmpty = true

    override val isVisibleDuringActionMode = true

    override val isSeparatorVisibleWhenNotExpanded = false
}
