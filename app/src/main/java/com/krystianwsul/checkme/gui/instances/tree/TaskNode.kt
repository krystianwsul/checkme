package com.krystianwsul.checkme.gui.instances.tree

import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.checkme.gui.tasks.ShowTaskActivity
import com.krystianwsul.checkme.gui.tree.AbstractHolder
import com.krystianwsul.checkme.gui.tree.AbstractModelNode
import com.krystianwsul.checkme.gui.tree.HolderType
import com.krystianwsul.checkme.gui.tree.delegates.expandable.ExpandableDelegate
import com.krystianwsul.checkme.gui.tree.delegates.indentation.IndentationDelegate
import com.krystianwsul.checkme.gui.tree.delegates.indentation.IndentationModelNode
import com.krystianwsul.checkme.gui.tree.delegates.invisible_checkbox.InvisibleCheckboxDelegate
import com.krystianwsul.checkme.gui.tree.delegates.invisible_checkbox.InvisibleCheckboxModelNode
import com.krystianwsul.checkme.gui.tree.delegates.multiline.MultiLineDelegate
import com.krystianwsul.checkme.gui.tree.delegates.multiline.MultiLineModelNode
import com.krystianwsul.checkme.gui.tree.delegates.multiline.MultiLineNameData
import com.krystianwsul.checkme.gui.tree.delegates.thumbnail.ThumbnailDelegate
import com.krystianwsul.checkme.gui.tree.delegates.thumbnail.ThumbnailModelNode
import com.krystianwsul.checkme.gui.utils.SearchData
import com.krystianwsul.common.utils.TaskKey
import com.krystianwsul.treeadapter.ModelNode
import com.krystianwsul.treeadapter.TreeNode

class TaskNode(
        override val indentation: Int,
        val taskData: GroupListDataWrapper.TaskData,
        private val taskParent: TaskParent,
        override val parentNode: ModelNode<AbstractHolder>?,
) :
        AbstractModelNode(),
        TaskParent,
        MultiLineModelNode,
        InvisibleCheckboxModelNode,
        ThumbnailModelNode,
        IndentationModelNode {

    override lateinit var treeNode: TreeNode<AbstractHolder>
        private set

    override val holderType = HolderType.EXPANDABLE_MULTILINE

    override val id = taskData.taskKey

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
                MultiLineDelegate(this),
                InvisibleCheckboxDelegate(this),
                ThumbnailDelegate(this),
                IndentationDelegate(this)
        )
    }

    override val widthKey
        get() = MultiLineDelegate.WidthKey(
                indentation,
                true,
                thumbnail != null,
                true
        )

    override val checkBoxInvisible = true

    fun initialize(
            parentTreeNode: TreeNode<AbstractHolder>,
            expandedTaskKeys: List<TaskKey>,
            selectedTaskKeys: List<TaskKey>,
    ): TreeNode<AbstractHolder> {
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

    override fun compareTo(other: ModelNode<AbstractHolder>) = (other as TaskNode).taskData.startExactTimeStamp.let {
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

            return text?.let { Pair(it, R.color.textSecondary) }
        }

    override fun onClick(holder: AbstractHolder) {
        groupListFragment.activity.startActivity(ShowTaskActivity.newIntent(taskData.taskKey))
    }

    override val isSelectable = true

    override val thumbnail = taskData.imageState

    override fun matches(filterCriteria: Any?) = taskData.matchesQuery((filterCriteria as? SearchData)?.query)

    override fun canBeShownWithFilterCriteria(filterCriteria: Any?) = false
}
