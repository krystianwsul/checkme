package com.krystianwsul.checkme.gui.instances.tree

import com.krystianwsul.checkme.domainmodel.MixedInstanceDataCollection
import com.krystianwsul.checkme.gui.tree.AbstractHolder
import com.krystianwsul.checkme.gui.utils.flatten
import com.krystianwsul.treeadapter.NodeContainer
import com.krystianwsul.treeadapter.TreeNode


class NotDoneGroupCollection(
    private val indentation: Int,
    private val nodeCollection: NodeCollection,
    private val nodeContainer: NodeContainer<AbstractHolder>,
) {

    private val notDoneGroupNodes = mutableListOf<NotDoneGroupNode>()

    fun initialize(
        mixedInstanceDataCollection: MixedInstanceDataCollection,
        contentDelegateStates: Map<NotDoneNode.ContentDelegate.Id, NotDoneNode.ContentDelegate.State>,
    ): List<TreeNode<AbstractHolder>> {
        val contentDelegates = mixedInstanceDataCollection.groupTypeTree.map {
            it.toContentDelegate(nodeCollection.groupAdapter, indentation, nodeCollection)
        }

        val nodePairs = contentDelegates.map {
            val notDoneGroupNode = NotDoneGroupNode(indentation, nodeCollection, it)

            val notDoneGroupTreeNode = notDoneGroupNode.initialize(contentDelegateStates, nodeContainer)

            notDoneGroupTreeNode to notDoneGroupNode
        }

        notDoneGroupNodes += nodePairs.map { it.second }

        return nodePairs.map { it.first }
    }

    val contentDelegateStates get() = notDoneGroupNodes.map { it.contentDelegate.states }.flatten()

}
