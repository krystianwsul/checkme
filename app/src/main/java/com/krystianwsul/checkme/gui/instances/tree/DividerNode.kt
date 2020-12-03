package com.krystianwsul.checkme.gui.instances.tree

import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.checkme.gui.tree.*
import com.krystianwsul.checkme.gui.tree.delegates.expandable.ExpandableDelegate
import com.krystianwsul.checkme.gui.tree.delegates.indentation.IndentationDelegate
import com.krystianwsul.checkme.gui.tree.delegates.indentation.IndentationModelNode
import com.krystianwsul.checkme.gui.tree.delegates.singleline.SingleLineDelegate
import com.krystianwsul.checkme.gui.tree.delegates.singleline.SingleLineModelNode
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.treeadapter.ModelNode
import com.krystianwsul.treeadapter.NodeContainer
import com.krystianwsul.treeadapter.TreeNode
import com.krystianwsul.treeadapter.TreeViewAdapter
import java.util.*

class DividerNode(
        override val indentation: Int,
        val nodeCollection: NodeCollection,
        override val parentNode: ModelNode<AbstractHolder>?,
) : AbstractModelNode(), SingleLineModelNode, IndentationModelNode {

    override val holderType = HolderType.EXPANDABLE_SINGLELINE

    override val id get() = Id(nodeCollection.nodeContainer.id)

    data class Id(val id: Any)

    override lateinit var treeNode: TreeNode<AbstractHolder>
        private set

    private val doneInstanceNodes = ArrayList<DoneInstanceNode>()

    private val groupAdapter get() = nodeCollection.groupAdapter

    private val groupListFragment get() = groupAdapter.groupListFragment

    override val delegates by lazy {
        listOf(
                ExpandableDelegate(treeNode),
                SingleLineDelegate(this),
                IndentationDelegate(this)
        )
    }

    override val disableRipple = true

    fun initialize(
            expanded: Boolean,
            nodeContainer: NodeContainer<AbstractHolder>,
            doneInstanceDatas: List<GroupListDataWrapper.InstanceData>,
            expandedInstances: Map<InstanceKey, Boolean>,
            selectedInstances: List<InstanceKey>,
    ): TreeNode<AbstractHolder> {
        check(!expanded || doneInstanceDatas.isNotEmpty())

        treeNode = TreeNode(this, nodeContainer, expanded, false)

        val childTreeNodes = doneInstanceDatas.map { newChildTreeNode(it, expandedInstances, selectedInstances) }

        treeNode.setChildTreeNodes(childTreeNodes)

        return treeNode
    }

    private fun newChildTreeNode(
            instanceData: GroupListDataWrapper.InstanceData,
            expandedInstances: Map<InstanceKey, Boolean>,
            selectedInstances: List<InstanceKey>,
    ): TreeNode<AbstractHolder> {
        checkNotNull(instanceData.done)

        val doneInstanceNode = DoneInstanceNode(indentation, instanceData, this)

        val childTreeNode = doneInstanceNode.initialize(treeNode, expandedInstances, selectedInstances)

        doneInstanceNodes.add(doneInstanceNode)

        return childTreeNode
    }

    fun expanded() = treeNode.isExpanded

    fun addExpandedInstances(expandedInstances: MutableMap<InstanceKey, Boolean>) {
        for (doneInstanceNode in doneInstanceNodes)
            doneInstanceNode.addExpandedInstances(expandedInstances)
    }

    override val text by lazy { groupListFragment.activity.getString(R.string.done) }

    fun remove(doneInstanceNode: DoneInstanceNode, placeholder: TreeViewAdapter.Placeholder) {
        check(doneInstanceNodes.contains(doneInstanceNode))
        doneInstanceNodes.remove(doneInstanceNode)

        treeNode.remove(doneInstanceNode.treeNode, placeholder)
    }

    fun add(
            instanceData: GroupListDataWrapper.InstanceData,
            placeholder: TreeViewAdapter.Placeholder
    ) = treeNode.add(newChildTreeNode(instanceData, mapOf(), listOf()), placeholder)

    override fun compareTo(other: ModelNode<AbstractHolder>): Int {
        check(
                other is AssignedNode
                        || other is NoteNode
                        || other is NotDoneGroupNode
                        || other is UnscheduledNode
                        || other is ImageNode
        )

        return 1
    }

    override val isVisibleWhenEmpty = false

    override fun matches(filterCriteria: Any?) = false

    override fun canBeShownWithFilterCriteria(filterCriteria: Any?): Boolean? = null
}
