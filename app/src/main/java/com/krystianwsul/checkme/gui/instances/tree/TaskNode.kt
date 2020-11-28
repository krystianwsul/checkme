package com.krystianwsul.checkme.gui.instances.tree

import android.view.View
import com.krystianwsul.checkme.databinding.RowListBinding
import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.checkme.gui.tasks.ShowTaskActivity
import com.krystianwsul.checkme.gui.tree.BaseHolder
import com.krystianwsul.checkme.gui.tree.GroupHolderNode
import com.krystianwsul.checkme.gui.tree.NodeType
import com.krystianwsul.checkme.gui.tree.RegularNodeHolder
import com.krystianwsul.checkme.gui.tree.checkable.CheckBoxState
import com.krystianwsul.checkme.gui.tree.checkable.CheckableDelegate
import com.krystianwsul.checkme.gui.tree.checkable.CheckableModelNode
import com.krystianwsul.checkme.gui.tree.expandable.ExpandableDelegate
import com.krystianwsul.checkme.gui.tree.multiline.MultiLineDelegate
import com.krystianwsul.checkme.gui.tree.multiline.MultiLineModelNode
import com.krystianwsul.checkme.gui.tree.multiline.MultiLineNameData
import com.krystianwsul.checkme.gui.utils.SearchData
import com.krystianwsul.common.utils.TaskKey
import com.krystianwsul.treeadapter.ModelNode
import com.krystianwsul.treeadapter.TreeNode

class TaskNode(
        indentation: Int,
        val taskData: GroupListDataWrapper.TaskData,
        private val taskParent: TaskParent,
        override val parentNode: ModelNode<BaseHolder>?,
) : GroupHolderNode(indentation), TaskParent, CheckableModelNode<BaseHolder>, MultiLineModelNode<BaseHolder> {

    override lateinit var treeNode: TreeNode<BaseHolder>
        private set

    override val nodeType = NodeType.UNSCHEDULED_TASK

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

    override val delegates by lazy {
        listOf(
                ExpandableDelegate(treeNode),
                CheckableDelegate(this),
                MultiLineDelegate(this)
        )
    }

    override val widthKey
        get() = MultiLineDelegate.WidthKey(
                indentation,
                checkBoxState.visibility == View.GONE,
                hasAvatar,
                thumbnail != null
        )

    fun initialize(
            parentTreeNode: TreeNode<BaseHolder>,
            expandedTaskKeys: List<TaskKey>,
            selectedTaskKeys: List<TaskKey>,
    ): TreeNode<BaseHolder> {
        val selected = selectedTaskKeys.contains(taskData.taskKey)
        val expanded = expandedTaskKeys.contains(taskData.taskKey) && taskData.children.isNotEmpty()

        treeNode = TreeNode(this, parentTreeNode, expanded, selected)

        treeNode.setChildTreeNodes(taskData.children.map { newChildTreeNode(it, expandedTaskKeys, selectedTaskKeys) })

        return treeNode
    }

    private fun newChildTreeNode(
            taskData: GroupListDataWrapper.TaskData,
            expandedTaskKeys: List<TaskKey>,
            selectedTaskKeys: List<TaskKey>
    ) = TaskNode(indentation + 1, taskData, this, this).let {
        taskNodes.add(it)

        it.initialize(treeNode, expandedTaskKeys, selectedTaskKeys)
    }

    override val groupAdapter by lazy { taskParent.groupAdapter }

    private fun expanded() = treeNode.isExpanded

    override fun compareTo(other: ModelNode<BaseHolder>) = (other as TaskNode).taskData.startExactTimeStamp.let {
        if (indentation == 0) {
            -taskData.startExactTimeStamp.compareTo(it)
        } else {
            taskData.startExactTimeStamp.compareTo(it)
        }
    }

    override val name get() = MultiLineNameData.Visible(taskData.name)

    override val children: Pair<String, Int>?
        get() {
            val text = treeNode.takeIf { !it.isExpanded }
                    ?.allChildren
                    ?.filter { it.modelNode is TaskNode && it.canBeShown() }
                    ?.map { it.modelNode as TaskNode }
                    ?.takeIf { it.isNotEmpty() }
                    ?.joinToString(", ") { it.taskData.name }
                    ?: taskData.note.takeIf { !it.isNullOrEmpty() }

            return text?.let { Pair(it, colorSecondary) }
        }

    override fun onClick(holder: BaseHolder) {
        groupListFragment.activity.startActivity(ShowTaskActivity.newIntent(taskData.taskKey))
    }

    override val checkBoxState = CheckBoxState.Invisible

    override val isSelectable = true

    override val thumbnail = taskData.imageState

    override fun matches(filterCriteria: Any?) = taskData.matchesQuery((filterCriteria as? SearchData)?.query)

    override fun canBeShownWithFilterCriteria(filterCriteria: Any?) = false

    class Holder(rowListBinding: RowListBinding) : RegularNodeHolder(rowListBinding)
}
