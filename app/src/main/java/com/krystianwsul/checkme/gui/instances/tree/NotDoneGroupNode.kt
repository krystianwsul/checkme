package com.krystianwsul.checkme.gui.instances.tree

import androidx.recyclerview.widget.RecyclerView
import com.krystianwsul.checkme.domainmodel.extensions.setInstanceDone
import com.krystianwsul.checkme.domainmodel.extensions.setOrdinal
import com.krystianwsul.checkme.domainmodel.update.AndroidDomainUpdater
import com.krystianwsul.checkme.gui.instances.ShowGroupActivity
import com.krystianwsul.checkme.gui.instances.ShowInstanceActivity
import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.checkme.gui.tree.AbstractHolder
import com.krystianwsul.checkme.gui.tree.DetailsNode
import com.krystianwsul.checkme.gui.tree.ImageNode
import com.krystianwsul.checkme.gui.tree.delegates.checkable.CheckBoxState
import com.krystianwsul.checkme.gui.tree.delegates.multiline.MultiLineDelegate
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.treeadapter.FilterCriteria
import com.krystianwsul.treeadapter.ModelNode
import com.krystianwsul.treeadapter.NodeContainer
import com.krystianwsul.treeadapter.TreeNode
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.kotlin.addTo
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

    public override lateinit var treeNode: TreeNode<AbstractHolder>
        private set

    private val notDoneInstanceNodes = ArrayList<NotDoneInstanceNode>()

    private var singleInstanceNodeCollection: NodeCollection? = null

    val exactTimeStamp: ExactTimeStamp.Local // todo project contentDelegate

    val singleInstanceData get() = instanceDatas.single()

    private val groupListFragment get() = groupAdapter.groupListFragment

    override val thumbnail get() = if (singleInstance()) singleInstanceData.imageState else null

    override val widthKey
        get() = MultiLineDelegate.WidthKey(
            indentation,
            checkBoxState != CheckBoxState.Gone,
            thumbnail != null,
            true
        )

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

        val (expansionState, doneExpansionState) = collectionState.run {
            instanceData?.let {
                expandedInstances[it.instanceKey] ?: CollectionExpansionState()
            } ?: CollectionExpansionState(expandedGroups[exactTimeStamp.toTimeStamp()], null)
        }

        val selected = collectionState.run {
            instanceData?.let { selectedInstances.contains(it.instanceKey) } ?: selectedGroups.contains(exactTimeStamp.long)
        }

        treeNode = TreeNode(this, nodeContainer, selected, expansionState)

        if (instanceData != null) {
            singleInstanceNodeCollection = NodeCollection(
                indentation + 1,
                groupAdapter,
                false,
                treeNode,
                instanceData.note,
                this,
                instanceData.projectInfo,
            )

            (contentDelegate as ContentDelegate.Instance).initialize(treeNode, singleInstanceNodeCollection!!)

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
            treeNode.setChildTreeNodes(
                instanceDatas.map {
                    newChildTreeNode(
                        it,
                        collectionState,
                        collectionState.selectedInstances.contains(it.instanceKey),
                    )
                }
            )

            (contentDelegate as ContentDelegate.Group).initialize(notDoneInstanceNodes)
        }

        return treeNode
    }

    fun singleInstance(): Boolean {
        check(instanceDatas.isNotEmpty())

        return instanceDatas.size == 1
    }

    override val groupAdapter by lazy { nodeCollection.groupAdapter }

    override val checkBoxState
        get() = if (singleInstance()) {
            if (groupListFragment.selectionCallback.hasActionMode || treeNode.isSelected/* drag hack */) {
                CheckBoxState.Invisible
            } else {
                val groupAdapter = nodeCollection.groupAdapter

                val checked = if (nodeCollection.useDoneNode) {
                    check(singleInstanceData.done == null)

                    false
                } else {
                    singleInstanceData.done != null
                }

                CheckBoxState.Visible(checked) {
                    val instanceKey = singleInstanceData.instanceKey

                    fun setDone(done: Boolean) = AndroidDomainUpdater.setInstanceDone(
                        groupAdapter.dataId.toFirst(),
                        instanceKey,
                        done,
                    )

                    setDone(true).observeOn(AndroidSchedulers.mainThread())
                        .andThen(Maybe.defer { groupListFragment.listener.showSnackbarDoneMaybe(1) })
                        .flatMapCompletable { setDone(false) }
                        .subscribe()
                        .addTo(groupListFragment.attachedToWindowDisposable)

                    /**
                     * todo it would be better to move all of this out of the node, and both handle the snackbar and
                     * the subscription there
                     */
                }
            }
        } else {
            if (treeNode.isExpanded) CheckBoxState.Gone else CheckBoxState.Invisible
        }

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

    override fun onClick(holder: AbstractHolder) {
        groupListFragment.activity.startActivity(
            if (singleInstance()) {
                ShowInstanceActivity.getIntent(groupListFragment.activity, singleInstanceData.instanceKey)
            } else {
                ShowGroupActivity.getIntent(
                    (treeNode.modelNode as NotDoneGroupNode).exactTimeStamp,
                    groupListFragment.activity
                )
            }
        )
    }

    override fun compareTo(other: ModelNode<AbstractHolder>) = when (other) {
        is ImageNode, is DetailsNode -> 1
        is NotDoneGroupNode -> {
            val timeStampComparison = exactTimeStamp.compareTo(other.exactTimeStamp)
            if (timeStampComparison != 0) {
                timeStampComparison
            } else {
                check(singleInstance())
                check(other.singleInstance())

                singleInstanceData.compareTo(other.singleInstanceData)
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

    val expansionState get() = treeNode.expansionState

    override fun getOrdinal() = singleInstanceData.ordinal

    override fun setOrdinal(ordinal: Double) {
        AndroidDomainUpdater.setOrdinal(
            groupListFragment.parameters.dataId.toFirst(),
            singleInstanceData.taskKey,
            ordinal,
        )
            .subscribe()
            .addTo(groupListFragment.attachedToWindowDisposable)
    }

    override val id: Any = if (nodeCollection.useGroups) {
        GroupId(instanceDatas.map { it.instanceKey }.toSet(), exactTimeStamp)
    } else {
        SingleId(singleInstanceData.instanceKey)
    }

    override val toggleDescendants get() = !singleInstance()

    override fun normalize() = instanceDatas.forEach { it.normalize() }

    override fun matchesFilterParams(filterParams: FilterCriteria.Full.FilterParams) =
        instanceDatas.any { it.matchesFilterParams(filterParams) }

    override fun getMatchResult(query: String) =
        ModelNode.MatchResult.fromBoolean(instanceDatas.any { it.matchesQuery(query) })

    override fun ordinalDesc() = if (singleInstance()) {
        singleInstanceData.run { "$name $ordinal" }
    } else {
        null
    }

    data class SingleId(val instanceKey: InstanceKey)

    class GroupId(val instanceKeys: Set<InstanceKey>, val exactTimeStamp: ExactTimeStamp.Local) {

        override fun hashCode() = 1

        override fun equals(other: Any?): Boolean {
            if (other === this)
                return true

            if (other !is GroupId)
                return false

            return instanceKeys == other.instanceKeys || exactTimeStamp == other.exactTimeStamp
        }
    }
}
