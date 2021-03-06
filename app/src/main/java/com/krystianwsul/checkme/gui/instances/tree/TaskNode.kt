package com.krystianwsul.checkme.gui.instances.tree

import com.krystianwsul.checkme.gui.tasks.ShowTaskActivity
import com.krystianwsul.common.utils.TaskKey
import com.krystianwsul.treeadapter.ModelNode
import com.krystianwsul.treeadapter.TreeNode

class TaskNode(
        indentation: Int,
        val taskData: GroupListFragment.TaskData,
        private val taskParent: TaskParent) : GroupHolderNode(indentation), TaskParent {

    override lateinit var treeNode: TreeNode<NodeHolder>
        private set

    override val id = taskData.taskKey

    override val ripple = true

    private val taskNodes = mutableListOf<TaskNode>()

    private val groupListFragment by lazy { groupAdapter.groupListFragment }

    val expandedTaskKeys: List<TaskKey>
        get() = if (taskNodes.isEmpty()) {
            check(!expanded())

            listOf()
        } else {
            mutableListOf<TaskKey>().apply {
                if (expanded())
                    add(taskData.taskKey)

                addAll(taskNodes.flatMap { it.expandedTaskKeys })
            }
        }

    fun initialize(
            parentTreeNode: TreeNode<NodeHolder>,
            expandedTaskKeys: List<TaskKey>,
            selectedTaskKeys: List<TaskKey>): TreeNode<NodeHolder> {
        val selected = selectedTaskKeys.contains(taskData.taskKey)
        val expanded = expandedTaskKeys.contains(taskData.taskKey) && taskData.children.isNotEmpty()

        treeNode = TreeNode(this, parentTreeNode, expanded, selected)

        treeNode.setChildTreeNodes(taskData.children.map { newChildTreeNode(it, expandedTaskKeys, selectedTaskKeys) })

        return treeNode
    }

    private fun newChildTreeNode(taskData: GroupListFragment.TaskData, expandedTaskKeys: List<TaskKey>, selectedTaskKeys: List<TaskKey>) = TaskNode(indentation + 1, taskData, this).let {
        taskNodes.add(it)

        it.initialize(treeNode, expandedTaskKeys, selectedTaskKeys)
    }

    override val groupAdapter by lazy { taskParent.groupAdapter }

    private fun expanded() = treeNode.isExpanded

    override fun compareTo(other: ModelNode<NodeHolder>) = (other as TaskNode).taskData.startExactTimeStamp.let {
        if (indentation == 0) {
            -taskData.startExactTimeStamp.compareTo(it)
        } else {
            taskData.startExactTimeStamp.compareTo(it)
        }
    }

    override val name get() = NameData(taskData.name)

    override val children
        get() = if ((taskData.children.isEmpty() || expanded()) && taskData.note.isNullOrEmpty()) {
            null
        } else {
            val text = if (!expanded() && taskData.children.isNotEmpty()) {
                taskData.children
                        .sortedBy { it.startExactTimeStamp }
                        .joinToString(", ") { it.name }
            } else {
                check(!taskData.note.isNullOrEmpty())

                taskData.note
            }

            val color = colorSecondary

            Pair(text, color)
        }

    override fun onClick(holder: NodeHolder) {
        groupListFragment.activity.startActivity(ShowTaskActivity.newIntent(taskData.taskKey))
    }

    override val checkBoxState = CheckBoxState.Invisible

    override val isSelectable = true

    override val thumbnail = taskData.imageState
}
