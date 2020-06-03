package com.krystianwsul.checkme.gui.instances.tree

import com.krystianwsul.checkme.R
import com.krystianwsul.common.utils.TaskKey
import com.krystianwsul.treeadapter.ModelNode
import com.krystianwsul.treeadapter.NodeContainer
import com.krystianwsul.treeadapter.TreeNode

class UnscheduledNode(private val nodeCollection: NodeCollection) : GroupHolderNode(0), TaskParent {

    override val id get() = Id(nodeCollection.nodeContainer.id)

    data class Id(val id: Any)

    private lateinit var taskDatas: List<GroupListDataWrapper.TaskData>

    override lateinit var treeNode: TreeNode<NodeHolder>
        private set

    private val taskNodes = mutableListOf<TaskNode>()

    val expandedTaskKeys get() = taskNodes.flatMap { it.expandedTaskKeys }

    private val groupListFragment by lazy { groupAdapter.groupListFragment }

    fun initialize(
            expanded: Boolean,
            nodeContainer: NodeContainer<NodeHolder>,
            taskDatas: List<GroupListDataWrapper.TaskData>,
            expandedTaskKeys: List<TaskKey>,
            selectedTaskKeys: List<TaskKey>): TreeNode<NodeHolder> {
        check(!expanded || taskDatas.isNotEmpty())

        this.taskDatas = taskDatas

        treeNode = TreeNode(this, nodeContainer, expanded, false)

        treeNode.setChildTreeNodes(taskDatas.map { newChildTreeNode(it, expandedTaskKeys, selectedTaskKeys) })

        return treeNode
    }

    private fun newChildTreeNode(
            taskData: GroupListDataWrapper.TaskData,
            expandedTaskKeys: List<TaskKey>,
            selectedTaskKeys: List<TaskKey>) = TaskNode(0, taskData, this).let {
        taskNodes.add(it)

        it.initialize(treeNode, expandedTaskKeys, selectedTaskKeys)
    }

    fun expanded() = treeNode.isExpanded

    override val groupAdapter by lazy { nodeCollection.groupAdapter }

    override fun compareTo(other: ModelNode<NodeHolder>) = if (other is DividerNode) {
        -1
    } else {
        check(other is NotDoneGroupNode)

        1
    }

    override val name get() = NameData(groupListFragment.activity.getString(R.string.noReminder))

    override val isVisibleWhenEmpty = false

    override val checkBoxState = CheckBoxState.Invisible
}
