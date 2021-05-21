package com.krystianwsul.checkme.gui.instances.tree

import androidx.recyclerview.widget.RecyclerView
import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.checkme.gui.instances.list.GroupListFragment
import com.krystianwsul.checkme.gui.tree.AbstractHolder
import com.krystianwsul.treeadapter.ModelNode
import com.krystianwsul.treeadapter.NodeContainer
import com.krystianwsul.treeadapter.TreeNode

class NotDoneInstanceNode(
    override val indentation: Int,
    val instanceData: GroupListDataWrapper.InstanceData,
    override val parentNode: ModelNode<AbstractHolder>,
    override val groupAdapter: GroupListFragment.GroupAdapter,
) : NotDoneNode(ContentDelegate.Instance(groupAdapter, instanceData)) {

    fun initialize(
        collectionState: CollectionState,
        nodeContainer: NodeContainer<AbstractHolder>,
    ): TreeNode<AbstractHolder> {
        val (expansionState, doneExpansionState) =
            collectionState.expandedInstances[instanceData.instanceKey] ?: CollectionExpansionState()

        val selected = collectionState.selectedInstances.contains(instanceData.instanceKey)

        val treeNode = TreeNode(this, nodeContainer, selected, expansionState)

        val nodeCollection = NodeCollection(
            indentation + 1,
            groupAdapter,
            false,
            treeNode,
            instanceData.note,
            this,
            instanceData.projectInfo,
        )

        (contentDelegate as ContentDelegate.Instance).initialize(
            groupAdapter,
            treeNode,
            nodeCollection
        ) // todo project contentDelegate

        treeNode.setChildTreeNodes(
            nodeCollection.initialize(
                instanceData.children.values,
                collectionState,
                doneExpansionState,
                listOf(),
                null,
                mapOf(),
                listOf(),
                null,
            )
        )

        return treeNode
    }

    override fun compareTo(other: ModelNode<AbstractHolder>) =
        instanceData.compareTo((other as NotDoneInstanceNode).instanceData)

    override fun tryStartDrag(viewHolder: RecyclerView.ViewHolder): Boolean {
        val groupListFragment = groupAdapter.groupListFragment

        return if (groupListFragment.parameters.groupListDataWrapper.taskEditable != false
            && groupAdapter.treeNodeCollection.selectedChildren.isEmpty()
            && treeNode.parent.displayedChildNodes.none { it.isExpanded }
        ) {
            groupListFragment.dragHelper.startDrag(viewHolder)

            true
        } else {
            false
        }
    }

    override val deselectParent = true
}