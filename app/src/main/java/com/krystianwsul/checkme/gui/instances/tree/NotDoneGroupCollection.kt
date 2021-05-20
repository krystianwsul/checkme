package com.krystianwsul.checkme.gui.instances.tree

import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.checkme.gui.tree.AbstractHolder
import com.krystianwsul.checkme.gui.utils.flatten
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.treeadapter.NodeContainer
import com.krystianwsul.treeadapter.TreeNode


class NotDoneGroupCollection(
    private val indentation: Int,
    private val nodeCollection: NodeCollection,
    private val nodeContainer: NodeContainer<AbstractHolder>,
) {

    private val notDoneGroupNodes = mutableListOf<NotDoneGroupNode>()

    val groupExpansionStates
        get() = notDoneGroupNodes.filter { !it.singleInstance() }
            .map { it.exactTimeStamp.toTimeStamp() to it.expansionState }
            .toMap()

    fun initialize(
        notDoneInstanceDatas: List<GroupListDataWrapper.InstanceData>,
        collectionState: CollectionState,
        selectedInstances: List<InstanceKey>,
        selectedGroups: List<Long>,
    ) = if (nodeCollection.useGroups) {
        notDoneInstanceDatas.groupBy { it.instanceTimeStamp }
            .values
            .map {
                newNotDoneGroupNode(
                    it.toMutableList(),
                    collectionState,
                    selectedInstances,
                    selectedGroups,
                )
            }
    } else {
        notDoneInstanceDatas.map {
            newNotDoneGroupNode(
                mutableListOf(it),
                collectionState,
                selectedInstances,
                selectedGroups,
            )
        }
    }

    private fun newNotDoneGroupNode(
        instanceDatas: MutableList<GroupListDataWrapper.InstanceData>,
        collectionState: CollectionState,
        selectedInstances: List<InstanceKey>,
        selectedGroups: List<Long>,
    ): TreeNode<AbstractHolder> {
        check(instanceDatas.isNotEmpty())

        val notDoneGroupNode = NotDoneGroupNode(indentation, nodeCollection, instanceDatas)

        val notDoneGroupTreeNode = notDoneGroupNode.initialize(
            collectionState,
            selectedInstances,
            selectedGroups,
            nodeContainer,
        )

        notDoneGroupNodes.add(notDoneGroupNode)

        return notDoneGroupTreeNode
    }

    val instanceExpansionStates get() = notDoneGroupNodes.map { it.instanceExpansionStates }.flatten()
}
