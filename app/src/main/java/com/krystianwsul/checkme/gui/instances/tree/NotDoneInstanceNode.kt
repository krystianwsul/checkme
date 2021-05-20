package com.krystianwsul.checkme.gui.instances.tree

import androidx.recyclerview.widget.RecyclerView
import com.krystianwsul.checkme.domainmodel.DomainListenerManager
import com.krystianwsul.checkme.domainmodel.extensions.setInstanceDone
import com.krystianwsul.checkme.domainmodel.extensions.setOrdinal
import com.krystianwsul.checkme.domainmodel.update.AndroidDomainUpdater
import com.krystianwsul.checkme.gui.instances.ShowInstanceActivity
import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.checkme.gui.instances.list.GroupListFragment
import com.krystianwsul.checkme.gui.tree.AbstractHolder
import com.krystianwsul.checkme.gui.tree.AbstractModelNode
import com.krystianwsul.checkme.gui.tree.DetailsNode
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
import com.krystianwsul.treeadapter.Sortable
import com.krystianwsul.treeadapter.TreeNode
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.kotlin.addTo

class NotDoneInstanceNode(
    override val indentation: Int,
    val instanceData: GroupListDataWrapper.InstanceData,
    override val parentNode: ModelNode<AbstractHolder>,
    override val groupAdapter: GroupListFragment.GroupAdapter,
) :
    AbstractModelNode(),
    NodeCollectionParent,
    CheckableModelNode,
    MultiLineModelNode,
    ThumbnailModelNode,
    IndentationModelNode,
    Sortable,
    DetailsNode.Parent {

    override val holderType = HolderType.CHECKABLE

    override val isSelectable = true

    public override lateinit var treeNode: TreeNode<AbstractHolder>
        private set

    private lateinit var nodeCollection: NodeCollection

    private val groupListFragment get() = groupAdapter.groupListFragment

    override val delegates by lazy {
        listOf(
            ExpandableDelegate(treeNode),
            CheckableDelegate(this),
            MultiLineDelegate(this),
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

    override val isDraggable = true

    fun initialize(
        expandedInstances: Map<InstanceKey, CollectionExpansionState>,
        selected: Boolean,
        selectedInstances: List<InstanceKey>,
        selectedGroups: List<Long>,
        notDoneGroupTreeNode: TreeNode<AbstractHolder>,
    ): TreeNode<AbstractHolder> {
        val (expansionState, doneExpansionState) =
            expandedInstances[instanceData.instanceKey] ?: CollectionExpansionState()

        treeNode = TreeNode(this, notDoneGroupTreeNode, selected, expansionState)

        nodeCollection = NodeCollection(
            indentation + 1,
            groupAdapter,
            false,
            treeNode,
            instanceData.note,
            this,
            instanceData.projectInfo,
        )

        treeNode.setChildTreeNodes(
            nodeCollection.initialize(
                instanceData.children.values,
                mapOf(),
                expandedInstances,
                doneExpansionState,
                selectedInstances,
                selectedGroups,
                listOf(),
                null,
                mapOf(),
                listOf(),
                null,
            )
        )

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

    override val rowsDelegate by lazy { InstanceRowsDelegate(instanceData, false) }

    override val checkBoxState
        get() = if (groupListFragment.selectionCallback.hasActionMode) {
            CheckBoxState.Invisible
        } else {
            CheckBoxState.Visible(false) {
                val instanceKey = instanceData.instanceKey

                AndroidDomainUpdater.setInstanceDone(
                    DomainListenerManager.NotificationType.First(groupAdapter.dataId),
                    instanceKey,
                    true
                )
                    .observeOn(AndroidSchedulers.mainThread())
                    .andThen(Maybe.defer { groupListFragment.listener.showSnackbarDoneMaybe(1) })
                    .flatMapCompletable {
                        AndroidDomainUpdater.setInstanceDone(
                            DomainListenerManager.NotificationType.First(groupAdapter.dataId),
                            instanceKey,
                            false,
                        )
                    }
                    .subscribe()
                    .addTo(groupListFragment.attachedToWindowDisposable)
            }
        }

    override fun onClick(holder: AbstractHolder) =
        groupListFragment.activity.startActivity(
            ShowInstanceActivity.getIntent(
                groupListFragment.activity,
                instanceData.instanceKey,
            )
        )

    override fun compareTo(other: ModelNode<AbstractHolder>) =
        instanceData.compareTo((other as NotDoneInstanceNode).instanceData)

    override val id = Id(instanceData.instanceKey)

    override val deselectParent get() = true

    override val thumbnail = instanceData.imageState

    override fun normalize() = instanceData.normalize()

    override fun matchesFilterParams(filterParams: FilterCriteria.Full.FilterParams) =
        instanceData.matchesFilterParams(filterParams)

    override fun getMatchResult(query: String) = ModelNode.MatchResult.fromBoolean(instanceData.matchesQuery(query))

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

    override fun getOrdinal() = instanceData.ordinal

    override fun setOrdinal(ordinal: Double) {
        AndroidDomainUpdater.setOrdinal(
            groupListFragment.parameters.dataId.toFirst(),
            instanceData.taskKey,
            ordinal,
        )
            .subscribe()
            .addTo(groupListFragment.attachedToWindowDisposable)
    }

    data class Id(val instanceKey: InstanceKey)
}