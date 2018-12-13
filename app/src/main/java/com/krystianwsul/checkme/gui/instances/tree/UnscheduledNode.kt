package com.krystianwsul.checkme.gui.instances.tree

import android.view.View
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.utils.TaskKey
import com.krystianwsul.treeadapter.ModelNode
import com.krystianwsul.treeadapter.NodeContainer
import com.krystianwsul.treeadapter.TreeNode

class UnscheduledNode(private val nodeCollection: NodeCollection) : GroupHolderNode(0), TaskParent {

    override val id get() = Id(nodeCollection.nodeContainer.id)

    data class Id(val id: Any)

    private lateinit var taskDatas: List<GroupListFragment.TaskData>

    override lateinit var treeNode: TreeNode
        private set

    private val taskNodes = mutableListOf<TaskNode>()

    val expandedTaskKeys get() = taskNodes.flatMap { it.expandedTaskKeys }

    private val groupListFragment by lazy { groupAdapter.mGroupListFragment }

    fun initialize(expanded: Boolean, nodeContainer: NodeContainer, taskDatas: List<GroupListFragment.TaskData>, expandedTaskKeys: List<TaskKey>): TreeNode {
        check(!expanded || !taskDatas.isEmpty())

        this.taskDatas = taskDatas

        treeNode = TreeNode(this, nodeContainer, expanded, false)

        treeNode.setChildTreeNodes(taskDatas.map { newChildTreeNode(it, expandedTaskKeys) })

        return treeNode
    }

    private fun newChildTreeNode(taskData: GroupListFragment.TaskData, expandedTaskKeys: List<TaskKey>) = TaskNode(0, taskData, this).let {
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

    override val name get() = Triple(groupListFragment.activity.getString(R.string.noReminder), colorPrimary, true)

    override val isSelectable = false

    override fun onClick() = Unit

    override val isVisibleWhenEmpty = false

    override val isVisibleDuringActionMode = false

    override val isSeparatorVisibleWhenNotExpanded = false

    override val checkBoxVisibility = View.INVISIBLE
}
