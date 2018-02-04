package com.krystianwsul.checkme.gui.instances.tree

import android.graphics.Color
import android.support.v4.content.ContextCompat
import android.view.View

import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.gui.tasks.ShowTaskActivity
import com.krystianwsul.checkme.utils.TaskKey
import com.krystianwsul.treeadapter.ModelNode
import com.krystianwsul.treeadapter.TreeNode

import junit.framework.Assert

internal class TaskNode(density: Float, indentation: Int, private val taskData: GroupListFragment.TaskData, private val taskParent: TaskParent) : GroupHolderNode(density, indentation), ModelNode, TaskParent {

    private lateinit var treeNode: TreeNode

    private val taskNodes = mutableListOf<TaskNode>()

    private val groupListFragment = groupAdapter.mGroupListFragment

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

    private fun newChildTreeNode(taskData: GroupListFragment.TaskData, expandedTaskKeys: List<TaskKey>?) = TaskNode(mDensity, mIndentation + 1, taskData, this).let {
        taskNodes.add(it)

        it.initialize(treeNode, expandedTaskKeys)
    }

    override fun getGroupAdapter() = taskParent.groupAdapter

    private fun expanded() = treeNode.expanded()

    override fun compareTo(other: ModelNode) = (other as TaskNode).taskData.mStartExactTimeStamp.let {
        if (mIndentation == 0) {
            -taskData.mStartExactTimeStamp.compareTo(it)
        } else {
            taskData.mStartExactTimeStamp.compareTo(it)
        }
    }

    override fun getNameVisibility() = View.VISIBLE

    override fun getName() = taskData.Name

    override fun getNameColor() = ContextCompat.getColor(groupListFragment.activity!!, R.color.textPrimary)

    override fun getNameSingleLine() = true

    override fun getDetailsVisibility() = View.GONE

    override fun getDetails() = throw UnsupportedOperationException()

    override fun getDetailsColor() = throw UnsupportedOperationException()

    override fun getChildrenVisibility() = if ((taskData.Children.isEmpty() || expanded()) && taskData.mNote.isNullOrEmpty()) {
        View.GONE
    } else {
        View.VISIBLE
    }

    override fun getChildren() = if (!expanded() && !taskData.Children.isEmpty()) {
        taskData.Children
                .sortedBy { it.mStartExactTimeStamp }
                .joinToString(", ") { it.Name }
    } else {
        Assert.assertTrue(!taskData.mNote.isNullOrEmpty())

        taskData.mNote!!
    }

    override fun getChildrenColor(): Int {
        Assert.assertTrue(!expanded() && !taskData.Children.isEmpty() || !taskData.mNote.isNullOrEmpty())

        return ContextCompat.getColor(groupListFragment.activity!!, R.color.textSecondary)
    }

    override fun getExpandVisibility() = if (taskData.Children.isEmpty()) {
        Assert.assertTrue(!treeNode.expandVisible)

        View.INVISIBLE
    } else {
        Assert.assertTrue(treeNode.expandVisible)

        View.VISIBLE
    }

    override fun getExpandImageResource(): Int {
        Assert.assertTrue(treeNode.expandVisible)

        Assert.assertTrue(!taskData.Children.isEmpty())

        return if (treeNode.expanded())
            R.drawable.ic_expand_less_black_36dp
        else
            R.drawable.ic_expand_more_black_36dp
    }

    override fun getExpandOnClickListener(): View.OnClickListener {
        Assert.assertTrue(!taskData.Children.isEmpty())
        Assert.assertTrue(treeNode.expandVisible)

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

    override fun onClick() {
        groupListFragment.activity!!.startActivity(ShowTaskActivity.newIntent(taskData.mTaskKey))
    }

    override fun visibleWhenEmpty() = true

    override fun visibleDuringActionMode() = true

    override fun separatorVisibleWhenNotExpanded() = false
}
