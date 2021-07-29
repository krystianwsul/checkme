package com.krystianwsul.checkme.gui.instances.tree

import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.checkme.gui.tasks.ShowTasksActivity
import com.krystianwsul.checkme.gui.tree.AbstractHolder
import com.krystianwsul.checkme.gui.tree.AbstractModelNode
import com.krystianwsul.checkme.gui.tree.DetailsNode
import com.krystianwsul.checkme.gui.tree.HolderType
import com.krystianwsul.checkme.gui.tree.delegates.expandable.ExpandableDelegate
import com.krystianwsul.checkme.gui.tree.delegates.indentation.IndentationDelegate
import com.krystianwsul.checkme.gui.tree.delegates.indentation.IndentationModelNode
import com.krystianwsul.checkme.gui.tree.delegates.singleline.SingleLineDelegate
import com.krystianwsul.checkme.gui.tree.delegates.singleline.SingleLineModelNode
import com.krystianwsul.checkme.gui.utils.flatten
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.TaskKey
import com.krystianwsul.treeadapter.ModelNode
import com.krystianwsul.treeadapter.NodeContainer
import com.krystianwsul.treeadapter.TreeNode

class UnscheduledNode(
    private val nodeCollection: NodeCollection,
    private val unscheduledFirst: Boolean,
    private val projectKey: ProjectKey.Shared?,
) : AbstractModelNode(), TaskParent, SingleLineModelNode, IndentationModelNode {

    override val holderType = HolderType.EXPANDABLE_SINGLELINE

    override val id get() = Id(nodeCollection.nodeContainer.id)

    data class Id(val id: Any)

    private lateinit var taskDatas: List<GroupListDataWrapper.TaskData>

    override lateinit var treeNode: TreeNode<AbstractHolder>
        private set

    private val taskNodes = mutableListOf<TaskNode>()

    val taskExpansionStates get() = taskNodes.map { it.taskExpansionStates }.flatten()

    private val groupListFragment by lazy { groupAdapter.groupListFragment }

    override val parentNode: ModelNode<AbstractHolder>? = null

    override val indentation = 0

    override val delegates by lazy {
        listOf(
            ExpandableDelegate(treeNode),
            SingleLineDelegate(this),
            IndentationDelegate(this)
        )
    }

    fun initialize(
        expansionState: TreeNode.ExpansionState?,
        nodeContainer: NodeContainer<AbstractHolder>,
        taskDatas: List<GroupListDataWrapper.TaskData>,
        taskExpansionStates: Map<TaskKey, TreeNode.ExpansionState>,
        selectedTaskKeys: List<TaskKey>,
    ): TreeNode<AbstractHolder> {
        this.taskDatas = taskDatas

        treeNode = TreeNode(
            this,
            nodeContainer,
            initialExpansionState = expansionState.takeIf { taskDatas.isNotEmpty() },
        )

        treeNode.setChildTreeNodes(taskDatas.map { newChildTreeNode(it, taskExpansionStates, selectedTaskKeys) })

        return treeNode
    }

    private fun newChildTreeNode(
        taskData: GroupListDataWrapper.TaskData,
        taskExpansionStates: Map<TaskKey, TreeNode.ExpansionState>,
        selectedTaskKeys: List<TaskKey>,
    ) = TaskNode(0, taskData, this, this).let {
        taskNodes.add(it)

        it.initialize(treeNode, taskExpansionStates, selectedTaskKeys)
    }

    val expansionState get() = treeNode.expansionState

    override val groupAdapter by lazy { nodeCollection.groupAdapter }

    override fun onClick(holder: AbstractHolder) = groupListFragment.activity.startActivity(
        ShowTasksActivity.newIntent(ShowTasksActivity.Parameters.Unscheduled(projectKey))
    )

    override fun compareTo(other: ModelNode<AbstractHolder>) = when {
        unscheduledFirst -> -1
        other is DetailsNode -> -1
        other is DividerNode -> -1
        else -> {
            check(other is NotDoneGroupNode)

            1
        }
    }

    override val text by lazy { groupListFragment.activity.getString(R.string.notes) }


    override fun isVisible(actionMode: Boolean, hasVisibleChildren: Boolean): Boolean {
        return hasVisibleChildren
    }
}
