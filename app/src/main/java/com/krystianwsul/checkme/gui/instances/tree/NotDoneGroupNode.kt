package com.krystianwsul.checkme.gui.instances.tree

import androidx.recyclerview.widget.RecyclerView
import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.checkme.gui.tree.AbstractHolder
import com.krystianwsul.checkme.gui.tree.DetailsNode
import com.krystianwsul.checkme.gui.tree.ImageNode
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.treeadapter.ModelNode
import com.krystianwsul.treeadapter.NodeContainer
import com.krystianwsul.treeadapter.TreeNode
import java.util.*

class NotDoneGroupNode(
    override val indentation: Int,
    private val nodeCollection: NodeCollection,
    val instanceDatas: MutableList<GroupListDataWrapper.InstanceData>,
) : NotDoneNode(
    instanceDatas.singleOrNull()
        ?.let(ContentDelegate::Instance)
        ?: ContentDelegate.Group(nodeCollection.groupAdapter, instanceDatas)
) {

    override val parentNode get() = nodeCollection.parentNode

    private val notDoneInstanceNodes = ArrayList<NotDoneInstanceNode>()

    private var singleInstanceNodeCollection: NodeCollection? = null

    val exactTimeStamp: ExactTimeStamp.Local // todo project contentDelegate

    val singleInstanceData get() = instanceDatas.single()

    init {
        check(instanceDatas.isNotEmpty())

        exactTimeStamp = instanceDatas.map { it.instanceTimeStamp }
            .distinct()
            .single()
            .toLocalExactTimeStamp()
    }

    fun initialize(
        collectionState: CollectionState,
        nodeContainer: NodeContainer<AbstractHolder>,
    ): TreeNode<AbstractHolder> {
        check(instanceDatas.isNotEmpty())

        val instanceData = instanceDatas.singleOrNull()

        if (instanceData != null) {
            val (expansionState, doneExpansionState) =
                collectionState.expandedInstances[instanceData.instanceKey] ?: CollectionExpansionState()

            val selected = collectionState.selectedInstances.contains(instanceData.instanceKey)

            val treeNode = TreeNode(this, nodeContainer, selected, expansionState)

            singleInstanceNodeCollection = NodeCollection(
                indentation + 1,
                groupAdapter,
                false,
                treeNode,
                instanceData.note,
                this,
                instanceData.projectInfo,
            )

            (contentDelegate as ContentDelegate.Instance).initialize(groupAdapter, treeNode, singleInstanceNodeCollection!!)

            treeNode.setChildTreeNodes(
                singleInstanceNodeCollection!!.initialize(
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
        } else {
            val expansionState = collectionState.expandedGroups[exactTimeStamp.toTimeStamp()]

            val selected = collectionState.selectedGroups.contains(exactTimeStamp.long)

            val treeNode = TreeNode(this, nodeContainer, selected, expansionState)

            treeNode.setChildTreeNodes(
                instanceDatas.map {
                    newChildTreeNode(
                        it,
                        collectionState,
                        collectionState.selectedInstances.contains(it.instanceKey),
                        treeNode,
                    )
                }
            )

            (contentDelegate as ContentDelegate.Group).initialize(treeNode, notDoneInstanceNodes)
        }

        return treeNode
    }

    fun singleInstance(): Boolean {
        check(instanceDatas.isNotEmpty())

        return instanceDatas.size == 1
    }

    override val groupAdapter by lazy { nodeCollection.groupAdapter }

    override fun tryStartDrag(viewHolder: RecyclerView.ViewHolder): Boolean {
        val groupListFragment = groupAdapter.groupListFragment

        return if (singleInstance()
            && groupListFragment.parameters.groupListDataWrapper.taskEditable != false
            && groupAdapter.treeNodeCollection.selectedChildren.isEmpty()
            && treeNode.parent.displayedChildNodes.none { it.isExpanded }
            && (groupListFragment.parameters.draggable || indentation != 0)
        ) {
            groupListFragment.dragHelper.startDrag(viewHolder)

            true
        } else {
            false
        }
    }

    override fun compareTo(other: ModelNode<AbstractHolder>) = when (other) {
        is ImageNode, is DetailsNode -> 1
        is NotDoneGroupNode -> {
            val timeStampComparison = exactTimeStamp.compareTo(other.exactTimeStamp)
            if (timeStampComparison != 0) {
                timeStampComparison
            } else {
                fun NotDoneGroupNode.instanceData() = (contentDelegate as ContentDelegate.Instance).instanceData

                instanceData().compareTo(other.instanceData())
            }
        }
        is UnscheduledNode -> if (nodeCollection.searchResults) 1 else -1
        is DividerNode -> -1
        else -> throw IllegalArgumentException()
    }

    private fun newChildTreeNode(
        instanceData: GroupListDataWrapper.InstanceData,
        collectionState: CollectionState,
        selected: Boolean,
        treeNode: TreeNode<AbstractHolder>,
    ): TreeNode<AbstractHolder> {
        val notDoneInstanceNode = NotDoneInstanceNode(
            indentation,
            instanceData,
            this,
            groupAdapter,
        )

        val childTreeNode = notDoneInstanceNode.initialize(collectionState, selected, treeNode)

        notDoneInstanceNodes.add(notDoneInstanceNode)

        return childTreeNode
    }
}
