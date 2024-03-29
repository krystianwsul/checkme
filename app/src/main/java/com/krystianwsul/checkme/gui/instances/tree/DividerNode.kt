package com.krystianwsul.checkme.gui.instances.tree

import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.domainmodel.GroupTypeFactory
import com.krystianwsul.checkme.gui.tree.*
import com.krystianwsul.checkme.gui.tree.delegates.expandable.ExpandableDelegate
import com.krystianwsul.checkme.gui.tree.delegates.indentation.IndentationDelegate
import com.krystianwsul.checkme.gui.tree.delegates.indentation.IndentationModelNode
import com.krystianwsul.checkme.gui.tree.delegates.singleline.SingleLineDelegate
import com.krystianwsul.checkme.gui.tree.delegates.singleline.SingleLineModelNode
import com.krystianwsul.checkme.gui.utils.flatten
import com.krystianwsul.treeadapter.ModelNode
import com.krystianwsul.treeadapter.NodeContainer
import com.krystianwsul.treeadapter.TreeNode

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
            IndentationDelegate(this),
        )
    }

    override val disableRipple = true

    fun initialize(
        initialExpansionState: TreeNode.ExpansionState?,
        nodeContainer: NodeContainer<AbstractHolder>,
        doneSingleBridges: List<GroupTypeFactory.SingleBridge>,
        contentDelegateStates: Map<NotDoneNode.ContentDelegate.Id, NotDoneNode.ContentDelegate.State>,
    ): TreeNode<AbstractHolder> {
        treeNode = TreeNode(
            this,
            nodeContainer,
            initialExpansionState = initialExpansionState.takeIf { doneSingleBridges.isNotEmpty() },
        )

        val childTreeNodes = doneSingleBridges.map { newChildTreeNode(it, contentDelegateStates) }

        treeNode.setChildTreeNodes(childTreeNodes)

        return treeNode
    }

    private fun newChildTreeNode(
        singleBridge: GroupTypeFactory.SingleBridge,
        contentDelegateStates: Map<NotDoneNode.ContentDelegate.Id, NotDoneNode.ContentDelegate.State>,
    ): TreeNode<AbstractHolder> {
        checkNotNull(singleBridge.instanceData.done)

        val doneInstanceNode = DoneInstanceNode(indentation, singleBridge, this)

        val childTreeNode = doneInstanceNode.initialize(contentDelegateStates, treeNode)

        doneInstanceNodes.add(doneInstanceNode)

        return childTreeNode
    }

    val expansionState get() = treeNode.getSaveExpansionState()

    val contentDelegateStates get() = doneInstanceNodes.map { it.contentDelegate.states }.flatten()

    override val text by lazy { groupListFragment.activity.getString(R.string.done) }

    override fun compareTo(other: ModelNode<AbstractHolder>): Int {
        check(
            other is NotDoneGroupNode
                    || other is UnscheduledNode
                    || other is ImageNode
                    || other is DetailsNode
        )

        if (nodeCollection.doneBeforeNotDone && other is NotDoneGroupNode) return -1

        return 1
    }

    override fun isVisible(actionMode: Boolean, hasVisibleChildren: Boolean): Boolean {
        return hasVisibleChildren
    }
}
