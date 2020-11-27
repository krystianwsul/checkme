package com.krystianwsul.checkme.gui.instances.tree

import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.checkme.gui.instances.tree.checkable.CheckBoxState
import com.krystianwsul.checkme.gui.instances.tree.checkable.CheckableDelegate
import com.krystianwsul.checkme.gui.instances.tree.checkable.CheckableModelNode
import com.krystianwsul.checkme.gui.instances.tree.expandable.ExpandableDelegate
import com.krystianwsul.checkme.gui.tasks.ShowTasksActivity
import com.krystianwsul.common.utils.TaskKey
import com.krystianwsul.treeadapter.ModelNode
import com.krystianwsul.treeadapter.NodeContainer
import com.krystianwsul.treeadapter.TreeNode

class UnscheduledNode(
        private val nodeCollection: NodeCollection,
        private val searchResults: Boolean,
) : GroupHolderNode(0), TaskParent, CheckableModelNode<NodeHolder> {

    override val id get() = Id(nodeCollection.nodeContainer.id)

    data class Id(val id: Any)

    private lateinit var taskDatas: List<GroupListDataWrapper.TaskData>

    override lateinit var treeNode: TreeNode<NodeHolder>
        private set

    private val taskNodes = mutableListOf<TaskNode>()

    val expandedTaskKeys get() = taskNodes.flatMap { it.expandedTaskKeys }

    private val groupListFragment by lazy { groupAdapter.groupListFragment }

    override val parentNode: ModelNode<NodeHolder>? = null

    override val delegates by lazy { listOf(ExpandableDelegate(treeNode), CheckableDelegate(this)) }

    fun initialize(
            expanded: Boolean,
            nodeContainer: NodeContainer<NodeHolder>,
            taskDatas: List<GroupListDataWrapper.TaskData>,
            expandedTaskKeys: List<TaskKey>,
            selectedTaskKeys: List<TaskKey>,
    ): TreeNode<NodeHolder> {
        check(!expanded || taskDatas.isNotEmpty())

        this.taskDatas = taskDatas

        treeNode = TreeNode(this, nodeContainer, expanded, false)

        treeNode.setChildTreeNodes(taskDatas.map { newChildTreeNode(it, expandedTaskKeys, selectedTaskKeys) })

        return treeNode
    }

    private fun newChildTreeNode(
            taskData: GroupListDataWrapper.TaskData,
            expandedTaskKeys: List<TaskKey>,
            selectedTaskKeys: List<TaskKey>
    ) = TaskNode(0, taskData, this, this).let {
        taskNodes.add(it)

        it.initialize(treeNode, expandedTaskKeys, selectedTaskKeys)
    }

    fun expanded() = treeNode.isExpanded

    override val groupAdapter by lazy { nodeCollection.groupAdapter }

    override fun onClick(holder: NodeHolder) = groupListFragment.activity.startActivity(ShowTasksActivity.newIntent(ShowTasksActivity.Parameters.Unscheduled))

    override fun compareTo(other: ModelNode<NodeHolder>) = when {
        searchResults -> -1
        other is DividerNode -> -1
        else -> {
            check(other is NotDoneGroupNode)

            1
        }
    }

    override val name get() = NameData.Visible(groupListFragment.activity.getString(R.string.noReminder))

    override val isVisibleWhenEmpty = false

    override val checkBoxState = CheckBoxState.Invisible

    override fun matches(filterCriteria: Any?) = false

    override fun canBeShownWithFilterCriteria(filterCriteria: Any?): Boolean? = null
}
