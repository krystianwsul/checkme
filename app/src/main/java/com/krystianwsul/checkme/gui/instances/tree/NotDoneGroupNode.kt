package com.krystianwsul.checkme.gui.instances.tree

import androidx.recyclerview.widget.RecyclerView
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.domainmodel.extensions.setInstanceDone
import com.krystianwsul.checkme.domainmodel.extensions.setOrdinal
import com.krystianwsul.checkme.domainmodel.update.AndroidDomainUpdater
import com.krystianwsul.checkme.gui.instances.ShowGroupActivity
import com.krystianwsul.checkme.gui.instances.ShowInstanceActivity
import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.checkme.gui.tree.AbstractHolder
import com.krystianwsul.checkme.gui.tree.DetailsNode
import com.krystianwsul.checkme.gui.tree.HolderType
import com.krystianwsul.checkme.gui.tree.ImageNode
import com.krystianwsul.checkme.gui.tree.delegates.checkable.CheckBoxState
import com.krystianwsul.checkme.gui.tree.delegates.checkable.CheckableDelegate
import com.krystianwsul.checkme.gui.tree.delegates.checkable.CheckableModelNode
import com.krystianwsul.checkme.gui.tree.delegates.expandable.ExpandableDelegate
import com.krystianwsul.checkme.gui.tree.delegates.indentation.IndentationDelegate
import com.krystianwsul.checkme.gui.tree.delegates.indentation.IndentationModelNode
import com.krystianwsul.checkme.gui.tree.delegates.multiline.MultiLineDelegate
import com.krystianwsul.checkme.gui.tree.delegates.multiline.MultiLineModelNode
import com.krystianwsul.checkme.gui.tree.delegates.multiline.MultiLineRow
import com.krystianwsul.checkme.gui.tree.delegates.thumbnail.ThumbnailDelegate
import com.krystianwsul.checkme.gui.tree.delegates.thumbnail.ThumbnailModelNode
import com.krystianwsul.checkme.gui.utils.flatten
import com.krystianwsul.checkme.utils.time.getDisplayText
import com.krystianwsul.common.time.DayOfWeek
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.HourMinute
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.treeadapter.*
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.kotlin.addTo
import java.util.*

class NotDoneGroupNode(
    override val indentation: Int,
    private val nodeCollection: NodeCollection,
    val instanceDatas: MutableList<GroupListDataWrapper.InstanceData>,
) :
    NotDoneNode(),
    NodeCollectionParent,
    Sortable,
    CheckableModelNode,
    MultiLineModelNode,
    ThumbnailModelNode,
    IndentationModelNode,
    DetailsNode.Parent {

    override val parentNode get() = nodeCollection.parentNode

    public override lateinit var treeNode: TreeNode<AbstractHolder>
        private set

    override val holderType = HolderType.CHECKABLE

    private val notDoneInstanceNodes = ArrayList<NotDoneInstanceNode>()

    private var singleInstanceNodeCollection: NodeCollection? = null

    val exactTimeStamp: ExactTimeStamp.Local

    val singleInstanceData get() = instanceDatas.single()

    private val groupListFragment get() = groupAdapter.groupListFragment

    override val thumbnail get() = if (singleInstance()) singleInstanceData.imageState else null

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
        }

        return treeNode
    }

    fun singleInstance(): Boolean {
        check(instanceDatas.isNotEmpty())

        return instanceDatas.size == 1
    }

    val instanceExpansionStates
        get(): Map<InstanceKey, CollectionExpansionState> {
            return if (singleInstance()) {
                val collectionExpansionState = CollectionExpansionState(
                    treeNode.expansionState,
                    singleInstanceNodeCollection!!.doneExpansionState
                )

                mapOf(singleInstanceData.instanceKey to collectionExpansionState) +
                        singleInstanceNodeCollection!!.instanceExpansionStates
            } else {
                notDoneInstanceNodes.map { it.instanceExpansionStates }.flatten()
            }
        }

    private inner class GroupRowsDelegate : DetailsNode.ProjectRowsDelegate(null, R.color.textSecondary) {

        private val details by lazy {
            val date = exactTimeStamp.date
            val hourMinute = exactTimeStamp.toTimeStamp().hourMinute

            val timeText = getCustomTimeData(date.dayOfWeek, hourMinute)?.name ?: hourMinute.toString()

            val text = date.getDisplayText() + ", " + timeText

            MultiLineRow.Visible(text, R.color.textSecondary)
        }

        override fun getRowsWithoutProject(isExpanded: Boolean, allChildren: List<TreeNode<*>>): List<MultiLineRow> {
            val name = if (isExpanded) {
                MultiLineRow.Invisible
            } else {
                MultiLineRow.Visible(
                    allChildren.filter { it.modelNode is NotDoneInstanceNode && it.canBeShown() }
                        .map { it.modelNode as NotDoneInstanceNode }
                        .sorted()
                        .joinToString(", ") { it.instanceData.name }
                )
            }

            return listOf(name, details)
        }
    }

    override val rowsDelegate by lazy {
        if (singleInstance())
            InstanceRowsDelegate(singleInstanceData)
        else
            GroupRowsDelegate()
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

    override val isDraggable = true

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

    private fun getCustomTimeData(dayOfWeek: DayOfWeek, hourMinute: HourMinute) =
        groupAdapter.customTimeDatas.firstOrNull { it.hourMinutes[dayOfWeek] == hourMinute }

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

    override val isSelectable = true

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
