package com.krystianwsul.checkme.gui.instances.tree

import android.support.v7.widget.RecyclerView
import com.krystianwsul.checkme.gui.tasks.ShowTaskActivity
import com.krystianwsul.checkme.utils.TaskKey
import com.krystianwsul.treeadapter.ModelNode
import com.krystianwsul.treeadapter.ModelState
import com.krystianwsul.treeadapter.TreeNode


class TaskNode(indentation: Int, val taskData: GroupListFragment.TaskData, private val taskParent: TaskParent) : GroupHolderNode(indentation), TaskParent {

    override lateinit var treeNode: TreeNode
        private set

    private val taskNodes = mutableListOf<TaskNode>()

    private val groupListFragment by lazy { groupAdapter.mGroupListFragment }

    val expandedTaskKeys: List<TaskKey>
        get() = if (taskNodes.isEmpty()) {
            check(!expanded())

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

    private fun newChildTreeNode(taskData: GroupListFragment.TaskData, expandedTaskKeys: List<TaskKey>?) = TaskNode(indentation + 1, taskData, this).let {
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

    override val name get() = Triple(taskData.Name, colorPrimary, true)

    override val children
        get() = if ((taskData.Children.isEmpty() || expanded()) && taskData.mNote.isNullOrEmpty()) {
            null
        } else {
            val text = if (!expanded() && !taskData.Children.isEmpty()) {
                taskData.Children
                        .sortedBy { it.mStartExactTimeStamp }
                        .joinToString(", ") { it.Name }
            } else {
                check(!taskData.mNote.isNullOrEmpty())

                taskData.mNote
            }

            val color = colorSecondary

            Pair(text, color)
        }

    override val expand
        get() = if (taskData.Children.isEmpty()) {
            null
        } else {
            Pair(treeNode.isExpanded, treeNode.expandListener)
        }

    override fun getOnLongClickListener(viewHolder: RecyclerView.ViewHolder) = treeNode.onLongClickListener

    override val onClickListener get() = treeNode.onClickListener

    override val isSelectable = false

    override fun onClick() {
        groupListFragment.activity.startActivity(ShowTaskActivity.newIntent(taskData.mTaskKey))
    }

    override val isVisibleWhenEmpty = true

    override val isVisibleDuringActionMode = true

    override val isSeparatorVisibleWhenNotExpanded = false

    override val state get() = State(taskData.copy())

    data class State(val taskData: GroupListFragment.TaskData) : ModelState {

        override fun same(other: ModelState) = (other as? State)?.taskData?.mTaskKey == taskData.mTaskKey
    }
}
