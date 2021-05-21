package com.krystianwsul.checkme.gui.instances.tree

import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.checkme.gui.tree.AbstractHolder
import com.krystianwsul.checkme.gui.tree.AbstractModelNode
import com.krystianwsul.checkme.gui.tree.DetailsNode
import com.krystianwsul.checkme.gui.tree.HolderType
import com.krystianwsul.checkme.gui.tree.delegates.checkable.CheckableDelegate
import com.krystianwsul.checkme.gui.tree.delegates.checkable.CheckableModelNode
import com.krystianwsul.checkme.gui.tree.delegates.expandable.ExpandableDelegate
import com.krystianwsul.checkme.gui.tree.delegates.indentation.IndentationDelegate
import com.krystianwsul.checkme.gui.tree.delegates.indentation.IndentationModelNode
import com.krystianwsul.checkme.gui.tree.delegates.multiline.MultiLineDelegate
import com.krystianwsul.checkme.gui.tree.delegates.multiline.MultiLineModelNode
import com.krystianwsul.checkme.gui.tree.delegates.thumbnail.ThumbnailDelegate
import com.krystianwsul.checkme.gui.tree.delegates.thumbnail.ThumbnailModelNode
import com.krystianwsul.checkme.gui.utils.flatten
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.treeadapter.Sortable
import com.krystianwsul.treeadapter.TreeNode

sealed class NotDoneNode(protected val contentDelegate: ContentDelegate) :
    AbstractModelNode(),
    NodeCollectionParent,
    Sortable,
    CheckableModelNode,
    MultiLineModelNode,
    ThumbnailModelNode,
    IndentationModelNode,
    DetailsNode.Parent {

    override val holderType = HolderType.CHECKABLE

    override val isSelectable = true

    override val isDraggable = true

    abstract override val treeNode: TreeNode<AbstractHolder>

    override val delegates by lazy {
        listOf(
            ExpandableDelegate(treeNode),
            CheckableDelegate(this),
            MultiLineDelegate(this),
            ThumbnailDelegate(this),
            IndentationDelegate(this)
        )
    }

    val instanceExpansionStates get() = contentDelegate.instanceExpansionStates

    protected sealed class ContentDelegate {

        abstract val instanceExpansionStates: Map<InstanceKey, CollectionExpansionState>

        class Instance(private val instanceData: GroupListDataWrapper.InstanceData) : ContentDelegate() {

            private lateinit var treeNode: TreeNode<*>
            private lateinit var nodeCollection: NodeCollection

            fun initialize(treeNode: TreeNode<*>, nodeCollection: NodeCollection) {
                this.treeNode = treeNode
                this.nodeCollection = nodeCollection
            }

            override val instanceExpansionStates: Map<InstanceKey, CollectionExpansionState>
                get() {
                    val collectionExpansionState = CollectionExpansionState(
                        treeNode.expansionState,
                        nodeCollection.doneExpansionState,
                    )

                    return mapOf(instanceData.instanceKey to collectionExpansionState) +
                            nodeCollection.instanceExpansionStates
                }
        }

        class Group : ContentDelegate() {

            private lateinit var notDoneInstanceNodes: List<NotDoneInstanceNode>

            fun initialize(notDoneInstanceNodes: List<NotDoneInstanceNode>) {
                this.notDoneInstanceNodes = notDoneInstanceNodes
            }

            override val instanceExpansionStates get() = notDoneInstanceNodes.map { it.instanceExpansionStates }.flatten()
        }
    }
}