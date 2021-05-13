package com.krystianwsul.checkme.gui.instances.tree

import com.krystianwsul.checkme.domainmodel.DomainListenerManager
import com.krystianwsul.checkme.domainmodel.extensions.setInstanceDone
import com.krystianwsul.checkme.domainmodel.update.AndroidDomainUpdater
import com.krystianwsul.checkme.gui.instances.ShowInstanceActivity
import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.checkme.gui.tree.AbstractHolder
import com.krystianwsul.checkme.gui.tree.AbstractModelNode
import com.krystianwsul.checkme.gui.tree.HolderType
import com.krystianwsul.checkme.gui.tree.delegates.checkable.CheckBoxState
import com.krystianwsul.checkme.gui.tree.delegates.checkable.CheckableDelegate
import com.krystianwsul.checkme.gui.tree.delegates.checkable.CheckableModelNode
import com.krystianwsul.checkme.gui.tree.delegates.expandable.ExpandableDelegate
import com.krystianwsul.checkme.gui.tree.delegates.indentation.IndentationDelegate
import com.krystianwsul.checkme.gui.tree.delegates.indentation.IndentationModelNode
import com.krystianwsul.checkme.gui.tree.delegates.multiline.MultiLineDelegate
import com.krystianwsul.checkme.gui.tree.delegates.multiline.MultiLineModelNode
import com.krystianwsul.checkme.gui.tree.delegates.thumbnail.ThumbnailDelegate
import com.krystianwsul.checkme.gui.tree.delegates.thumbnail.ThumbnailModelNode
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.treeadapter.FilterCriteria
import com.krystianwsul.treeadapter.ModelNode
import com.krystianwsul.treeadapter.TreeNode
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.kotlin.addTo

class DoneInstanceNode(
        override val indentation: Int,
        val instanceData: GroupListDataWrapper.InstanceData,
        private val dividerNode: DividerNode,
) : AbstractModelNode(),
        NodeCollectionParent,
        CheckableModelNode,
        MultiLineModelNode,
        ThumbnailModelNode,
        IndentationModelNode {

    public override lateinit var treeNode: TreeNode<AbstractHolder>
        private set

    override val holderType = HolderType.CHECKABLE

    private lateinit var nodeCollection: NodeCollection

    private val parentNodeCollection get() = dividerNode.nodeCollection

    private val groupListFragment get() = groupAdapter.groupListFragment

    override val thumbnail = instanceData.imageState

    override val parentNode = dividerNode

    override val delegates by lazy {
        listOf(
                ExpandableDelegate(treeNode),
                CheckableDelegate(this),
                MultiLineDelegate(this),
                ThumbnailDelegate(this),
                IndentationDelegate(this)
        )
    }

    fun initialize(
            dividerTreeNode: TreeNode<AbstractHolder>,
            expandedInstances: Map<InstanceKey, CollectionExpansionState>,
            selectedInstances: List<InstanceKey>,
    ): TreeNode<AbstractHolder> {
        val selected = selectedInstances.contains(instanceData.instanceKey)

        val (expansionState, doneExpansionState) =
                expandedInstances[instanceData.instanceKey] ?: CollectionExpansionState()

        treeNode = TreeNode(this, dividerTreeNode, selected, expansionState)

        nodeCollection = NodeCollection(
                indentation + 1,
                groupAdapter,
                false,
                treeNode,
                instanceData.note,
                this,
                instanceData.projectInfo
        )

        treeNode.setChildTreeNodes(nodeCollection.initialize(
                instanceData.children.values,
                mapOf(),
                expandedInstances,
                doneExpansionState,
                listOf(),
                listOf(),
                listOf(),
                null,
                mapOf(),
                listOf(),
                null))

        return treeNode
    }

    val instanceExpansionStates: Map<InstanceKey, CollectionExpansionState>
        get() {
            val collectionExpansionState = CollectionExpansionState(
                treeNode.expansionState,
                nodeCollection.doneExpansionState,
            )

            return mapOf(instanceData.instanceKey to collectionExpansionState) + nodeCollection.instanceExpansionStates
        }

    override val groupAdapter by lazy { parentNodeCollection.groupAdapter }

    override val rowsDelegate by lazy { InstanceRowsDelegate(instanceData) }

    override val checkBoxState
        get() = if (groupListFragment.selectionCallback.hasActionMode) {
            CheckBoxState.Invisible
        } else {
            CheckBoxState.Visible(true) {
                val nodeCollection = dividerNode.nodeCollection
                val groupAdapter = nodeCollection.groupAdapter

                AndroidDomainUpdater.setInstanceDone(
                        DomainListenerManager.NotificationType.First(groupAdapter.dataId),
                        instanceData.instanceKey,
                        false,
                )
                        .observeOn(AndroidSchedulers.mainThread())
                        .andThen(Maybe.defer { groupListFragment.listener.showSnackbarNotDoneMaybe(1) })
                        .flatMapCompletable {
                            AndroidDomainUpdater.setInstanceDone(
                                    DomainListenerManager.NotificationType.First(groupAdapter.dataId),
                                    instanceData.instanceKey,
                                    true,
                            )
                        }
                        .subscribe()
                        .addTo(groupListFragment.attachedToWindowDisposable)
            }
        }

    override val widthKey
        get() = MultiLineDelegate.WidthKey(
                indentation,
                true,
                thumbnail != null,
                true
        )

    override fun compareTo(other: ModelNode<AbstractHolder>): Int {
        checkNotNull(instanceData.done)

        val doneInstanceNode = other as DoneInstanceNode
        checkNotNull(doneInstanceNode.instanceData.done)

        return -instanceData.done.compareTo(doneInstanceNode.instanceData.done) // negate
    }

    override val isSelectable = true

    override fun onClick(holder: AbstractHolder) = groupListFragment.activity.startActivity(
            ShowInstanceActivity.getIntent(groupListFragment.activity, instanceData.instanceKey)
    )

    override val id = instanceData.instanceKey

    override fun normalize() = instanceData.normalize()

    override fun matchesFilterParams(filterParams: FilterCriteria.Full.FilterParams) =
            instanceData.matchesFilterParams(filterParams)

    override fun getMatchResult(query: String) = ModelNode.MatchResult.fromBoolean(instanceData.matchesQuery(query))
}
