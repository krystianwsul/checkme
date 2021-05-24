package com.krystianwsul.checkme.gui.instances.tree

import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
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

    val groupExpansionStates
        get() = notDoneGroupNodes.map { it.contentDelegate }
            .filterIsInstance<NotDoneNode.ContentDelegate.Group>()
            .map { it.timeStamp to it.treeNode.expansionState }
            .toMap()

    fun initialize(
        notDoneInstanceDatas: List<GroupListDataWrapper.InstanceData>,
        collectionState: CollectionState,
    ): List<TreeNode<AbstractHolder>> {
        val contentDelegates = GroupType.getContentDelegates(
            notDoneInstanceDatas,
            nodeCollection.groupingMode,
            nodeCollection.groupAdapter,
            indentation,
            nodeCollection,
        )

        val nodePairs = contentDelegates.map {
            val notDoneGroupNode = NotDoneGroupNode(indentation, nodeCollection, it)

            val notDoneGroupTreeNode = notDoneGroupNode.initialize(collectionState, nodeContainer)

            notDoneGroupTreeNode to notDoneGroupNode
        }

        notDoneGroupNodes += nodePairs.map { it.second }

        return nodePairs.map { it.first }
    }

    val instanceExpansionStates get() = notDoneGroupNodes.map { it.instanceExpansionStates }.flatten()
}
