package com.krystianwsul.checkme.gui.instances.tree

import androidx.recyclerview.widget.RecyclerView
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.extensions.setOrdinal
import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.checkme.gui.tasks.ShowTaskActivity
import com.krystianwsul.checkme.gui.tree.AbstractHolder
import com.krystianwsul.checkme.gui.tree.AbstractModelNode
import com.krystianwsul.checkme.gui.tree.DetailsNode
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
import com.krystianwsul.checkme.gui.utils.flatten
import com.krystianwsul.common.utils.TaskKey
import com.krystianwsul.treeadapter.*

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
        IndentationModelNode,
        Sortable {

    companion object {

        fun getTaskChildren(
                treeNode: TreeNode<*>,
                note: String?,
                getChildName: (treeNode: TreeNode<*>) -> String?,
        ): String? {
            return if (treeNode.isExpanded) {
                null
            } else {
                treeNode.allChildren
                        .filter { it.canBeShown() }
                        .mapNotNull(getChildName)
                        .takeIf { it.isNotEmpty() }
                        ?.joinToString(", ")
                        ?: note.takeIf { !it.isNullOrEmpty() }
            }
        }
    }

    override lateinit var treeNode: TreeNode<AbstractHolder>
        private set

    override val holderType = HolderType.EXPANDABLE_MULTILINE

    override val id = taskData.taskKey

    private val taskNodes = mutableListOf<TaskNode>()

    private val groupListFragment by lazy { groupAdapter.groupListFragment }

    val taskExpansionStates: Map<TaskKey, TreeNode.ExpansionState>
        get() =
            mapOf(taskData.taskKey to treeNode.expansionState) + taskNodes.map { it.taskExpansionStates }.flatten()

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

    override val isDraggable = true

    fun initialize(
            nodeContainer: NodeContainer<AbstractHolder>,
            taskExpansionStates: Map<TaskKey, TreeNode.ExpansionState>,
            selectedTaskKeys: List<TaskKey>,
    ): TreeNode<AbstractHolder> {
        treeNode = TreeNode(
                this,
                nodeContainer,
                selectedTaskKeys.contains(taskData.taskKey),
                taskExpansionStates[taskData.taskKey],
        )

        val treeNodes = mutableListOf<TreeNode<AbstractHolder>>()

        if (taskData.projectInfo != null || !taskData.note.isNullOrEmpty()) {
            treeNodes += DetailsNode(
                    taskData.projectInfo,
                    taskData.note,
                    this,
                    indentation + 1
            ).initialize(nodeContainer)
        }

        treeNodes += taskData.children.map { newChildTreeNode(it, taskExpansionStates, selectedTaskKeys) }

        treeNode.setChildTreeNodes(treeNodes)

        return treeNode
    }

    private fun newChildTreeNode(
            taskData: GroupListDataWrapper.TaskData,
            taskExpansionStates: Map<TaskKey, TreeNode.ExpansionState>,
            selectedTaskKeys: List<TaskKey>,
    ) = TaskNode(indentation + 1, taskData, this, this).let {
        taskNodes.add(it)

        it.initialize(treeNode, taskExpansionStates, selectedTaskKeys)
    }

    override val groupAdapter by lazy { taskParent.groupAdapter }

    private fun expanded() = treeNode.isExpanded

    override fun compareTo(other: ModelNode<AbstractHolder>) = if (other is TaskNode) {
        other.taskData
                .startExactTimeStamp
                .let {
                    if (indentation == 0) {
                        -taskData.startExactTimeStamp.compareTo(it)
                    } else {
                        taskData.startExactTimeStamp.compareTo(it)
                    }
                }
    } else {
        1
    }

    override val name get() = MultiLineNameData.Visible(taskData.name)

    override val children
        get() = getTaskChildren(treeNode, taskData.note) {
            (it.modelNode as? TaskNode)?.taskData?.name
        }?.let { Pair(it, R.color.textSecondary) }

    override fun onClick(holder: AbstractHolder) {
        groupListFragment.activity.startActivity(ShowTaskActivity.newIntent(taskData.taskKey))
    }

    override val isSelectable = true

    override val thumbnail = taskData.imageState

    override fun matchesFilterParams(filterParams: FilterCriteria.Full.FilterParams) =
            taskData.matchesFilterParams(filterParams)

    override fun getMatchResult(query: String) = ModelNode.MatchResult.fromBoolean(taskData.matchesQuery(query))

    override fun tryStartDrag(viewHolder: RecyclerView.ViewHolder): Boolean {
        return if (groupAdapter.treeNodeCollection.selectedChildren.isEmpty()
                && treeNode.parent.displayedChildNodes.none { it.isExpanded }
        ) {
            groupListFragment.dragHelper.startDrag(viewHolder)

            true
        } else {
            false
        }
    }

    override fun getOrdinal() = taskData.ordinal

    override fun setOrdinal(ordinal: Double) {
        taskData.ordinal = ordinal

        DomainFactory.instance.setOrdinal(groupListFragment.parameters.dataId, taskData.taskKey, ordinal)
    }
}
