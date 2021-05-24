package com.krystianwsul.checkme.gui.instances.tree

import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.domainmodel.extensions.setInstanceDone
import com.krystianwsul.checkme.domainmodel.extensions.setOrdinal
import com.krystianwsul.checkme.domainmodel.update.AndroidDomainUpdater
import com.krystianwsul.checkme.gui.instances.ShowGroupActivity
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
import com.krystianwsul.checkme.gui.tree.delegates.multiline.MultiLineRow
import com.krystianwsul.checkme.gui.tree.delegates.thumbnail.ThumbnailDelegate
import com.krystianwsul.checkme.gui.tree.delegates.thumbnail.ThumbnailModelNode
import com.krystianwsul.checkme.gui.utils.flatten
import com.krystianwsul.checkme.utils.time.getDisplayText
import com.krystianwsul.common.firebase.models.ImageState
import com.krystianwsul.common.time.DayOfWeek
import com.krystianwsul.common.time.HourMinute
import com.krystianwsul.common.time.TimeStamp
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.treeadapter.*
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.kotlin.addTo

sealed class NotDoneNode(val contentDelegate: ContentDelegate) :
    AbstractModelNode(),
    NodeCollectionParent,
    Sortable by contentDelegate,
    CheckableModelNode by contentDelegate,
    MultiLineModelNode,
    ThumbnailModelNode by contentDelegate,
    IndentationModelNode,
    DetailsNode.Parent {

    final override val holderType = HolderType.CHECKABLE

    final override val isSelectable = true

    final override val treeNode get() = contentDelegate.treeNode

    final override val delegates by lazy {
        listOf(
            ExpandableDelegate(treeNode),
            CheckableDelegate(this),
            MultiLineDelegate(this),
            ThumbnailDelegate(this),
            IndentationDelegate(this)
        )
    }

    final override val rowsDelegate get() = contentDelegate.rowsDelegate

    val instanceExpansionStates get() = contentDelegate.instanceExpansionStates

    final override val widthKey
        get() = MultiLineDelegate.WidthKey(
            indentation,
            checkBoxState != CheckBoxState.Gone,
            thumbnail != null,
            true,
        )

    override val id get() = contentDelegate.id

    final override val toggleDescendants = contentDelegate.toggleDescendants

    fun initialize(collectionState: CollectionState, nodeContainer: NodeContainer<AbstractHolder>) =
        contentDelegate.initialize(collectionState, nodeContainer, this)

    final override fun onClick(holder: AbstractHolder) = contentDelegate.onClick(holder)

    final override fun normalize() = contentDelegate.normalize()

    final override fun matchesFilterParams(filterParams: FilterCriteria.Full.FilterParams) =
        contentDelegate.matchesFilterParams(filterParams)

    final override fun getMatchResult(query: String) = contentDelegate.getMatchResult(query)

    sealed class ContentDelegate : ThumbnailModelNode, Sortable, CheckableModelNode, Comparable<ContentDelegate> {

        abstract val instanceDatas: List<GroupListDataWrapper.InstanceData> // todo project instanceDatas
        protected abstract val groupAdapter: GroupListFragment.GroupAdapter
        protected val groupListFragment get() = groupAdapter.groupListFragment

        abstract val rowsDelegate: DetailsNode.ProjectRowsDelegate
        abstract val instanceExpansionStates: Map<InstanceKey, CollectionExpansionState>
        abstract val treeNode: TreeNode<AbstractHolder>
        abstract val id: Any
        abstract val toggleDescendants: Boolean

        val timeStamp by lazy { // todo project instanceDatas will this be needed after other changes?
            instanceDatas.map { it.instanceTimeStamp } // todo project instanceDatas
                .distinct()
                .single()
        }

        abstract fun initialize(
            collectionState: CollectionState,
            nodeContainer: NodeContainer<AbstractHolder>,
            modelNode: DetailsNode.Parent,
        ): TreeNode<AbstractHolder>

        abstract fun onClick(holder: AbstractHolder)
        abstract fun normalize()
        abstract fun matchesFilterParams(filterParams: FilterCriteria.Full.FilterParams): Boolean
        abstract fun getMatchResult(query: String): ModelNode.MatchResult

        override fun compareTo(other: ContentDelegate): Int {
            fun ContentDelegate.instanceData() = instanceDatas.first() // todo project instanceDatas

            return instanceData().compareTo(other.instanceData())
        }

        class Instance(
            override val groupAdapter: GroupListFragment.GroupAdapter,
            val instanceData: GroupListDataWrapper.InstanceData,
            private val indentation: Int,
            showDetails: Boolean = true,
        ) : ContentDelegate() {

            override val instanceDatas = listOf(instanceData)

            override lateinit var treeNode: TreeNode<AbstractHolder>
            private lateinit var nodeCollection: NodeCollection

            override fun initialize(
                collectionState: CollectionState,
                nodeContainer: NodeContainer<AbstractHolder>,
                modelNode: DetailsNode.Parent,
            ): TreeNode<AbstractHolder> {
                val (expansionState, doneExpansionState) =
                    collectionState.expandedInstances[instanceData.instanceKey] ?: CollectionExpansionState()

                treeNode = TreeNode(
                    modelNode,
                    nodeContainer,
                    collectionState.selectedInstances.contains(instanceData.instanceKey),
                    expansionState,
                )

                nodeCollection = NodeCollection(
                    indentation + 1,
                    groupAdapter,
                    NodeCollection.GroupingMode.NONE, // todo project
                    treeNode,
                    instanceData.note,
                    modelNode,
                    instanceData.projectInfo,
                )

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

            override val rowsDelegate = InstanceRowsDelegate(instanceData, showDetails)

            override val instanceExpansionStates: Map<InstanceKey, CollectionExpansionState>
                get() {
                    val collectionExpansionState = CollectionExpansionState(
                        treeNode.expansionState,
                        nodeCollection.doneExpansionState,
                    )

                    return mapOf(instanceData.instanceKey to collectionExpansionState) +
                            nodeCollection.instanceExpansionStates
                }

            override val thumbnail = instanceData.imageState

            override val checkBoxState
                get() = if (groupListFragment.selectionCallback.hasActionMode || treeNode.isSelected/* drag hack */) {
                    CheckBoxState.Invisible
                } else {
                    val done = instanceData.done != null

                    CheckBoxState.Visible(done) {
                        fun setDone(done: Boolean) = AndroidDomainUpdater.setInstanceDone(
                            groupAdapter.dataId.toFirst(),
                            instanceData.instanceKey,
                            done,
                        )

                        setDone(!done).observeOn(AndroidSchedulers.mainThread())
                            .andThen(Maybe.defer { groupListFragment.listener.showSnackbarDoneMaybe(1) })
                            .flatMapCompletable { setDone(done) }
                            .subscribe()
                            .addTo(groupListFragment.attachedToWindowDisposable)

                        /**
                         * todo it would be better to move all of this out of the node, and both handle the snackbar and
                         * the subscription there
                         */
                    }
                }

            override val id: Any by lazy { Id(instanceData.instanceKey) }

            override val toggleDescendants = false

            override fun onClick(holder: AbstractHolder) = groupListFragment.activity.let {
                it.startActivity(ShowInstanceActivity.getIntent(it, instanceData.instanceKey))
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

            override fun normalize() = instanceData.normalize()

            override fun matchesFilterParams(filterParams: FilterCriteria.Full.FilterParams) =
                instanceData.matchesFilterParams(filterParams)

            override fun getMatchResult(query: String) = ModelNode.MatchResult.fromBoolean(instanceData.matchesQuery(query))

            private data class Id(val instanceKey: InstanceKey)
        }

        class Group(
            override val groupAdapter: GroupListFragment.GroupAdapter,
            override val instanceDatas: List<GroupListDataWrapper.InstanceData>,
            private val indentation: Int,
            private val groupingMode: NodeCollection.GroupingMode,
            private val nodeCollection: NodeCollection,
        ) : ContentDelegate() {

            init {
                check(instanceDatas.size > 1) // todo project instanceDatas
            }

            override val rowsDelegate: DetailsNode.ProjectRowsDelegate =
                GroupRowsDelegate(groupAdapter, timeStamp)

            override lateinit var treeNode: TreeNode<AbstractHolder>
            private lateinit var notDoneNodes: List<NotDoneNode>

            override fun initialize(
                collectionState: CollectionState,
                nodeContainer: NodeContainer<AbstractHolder>,
                modelNode: DetailsNode.Parent,
            ): TreeNode<AbstractHolder> {
                treeNode = TreeNode(
                    modelNode,
                    nodeContainer,
                    collectionState.selectedGroups.contains(timeStamp.long),
                    collectionState.expandedGroups[timeStamp],
                )

                val nodePairs = GroupType.getContentDelegates(
                    instanceDatas, // todo project instanceDatas
                    groupingMode,
                    groupAdapter,
                    indentation,
                    nodeCollection,
                ).map {
                    val notDoneGroupNode = NotDoneGroupNode(indentation, nodeCollection, it)

                    val childTreeNode = notDoneGroupNode.initialize(collectionState, treeNode)

                    childTreeNode to notDoneGroupNode
                }

                val (childTreeNodes, notDoneNodes) = nodePairs.unzip()
                treeNode.setChildTreeNodes(childTreeNodes)
                this.notDoneNodes = notDoneNodes

                return treeNode
            }

            override val instanceExpansionStates
                get() = notDoneNodes.map { it.contentDelegate.instanceExpansionStates }.flatten()

            override val thumbnail: ImageState? = null

            override val checkBoxState get() = if (treeNode.isExpanded) CheckBoxState.Gone else CheckBoxState.Invisible

            override val id: Any by lazy {
                Id(
                    instanceDatas.map { it.instanceKey }.toSet(),
                    timeStamp
                )
            } // todo project instanceDatas

            override val toggleDescendants = true

            override fun onClick(holder: AbstractHolder) =
                groupListFragment.activity.let { it.startActivity(ShowGroupActivity.getIntent(timeStamp, it)) }

            override fun getOrdinal(): Double = throw UnsupportedOperationException()
            override fun setOrdinal(ordinal: Double) = throw UnsupportedOperationException()

            override fun normalize() = instanceDatas.forEach { it.normalize() } // todo project instanceDatas

            override fun matchesFilterParams(filterParams: FilterCriteria.Full.FilterParams) =
                instanceDatas.any { it.matchesFilterParams(filterParams) } // todo project instanceDatas

            override fun getMatchResult(query: String) =
                ModelNode.MatchResult.fromBoolean(instanceDatas.any { it.matchesQuery(query) }) // todo project instanceDatas

            private class GroupRowsDelegate(
                private val groupAdapter: GroupListFragment.GroupAdapter,
                private val timeStamp: TimeStamp,
            ) : DetailsNode.ProjectRowsDelegate(null, R.color.textSecondary) {

                private fun getCustomTimeData(dayOfWeek: DayOfWeek, hourMinute: HourMinute) =
                    groupAdapter.customTimeDatas.firstOrNull { it.hourMinutes[dayOfWeek] == hourMinute }

                private val details by lazy {
                    val date = timeStamp.date
                    val hourMinute = timeStamp.hourMinute

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

            private class Id(val instanceKeys: Set<InstanceKey>, val timeStamp: TimeStamp) {

                override fun hashCode() = 1

                override fun equals(other: Any?): Boolean {
                    if (other === this) return true

                    if (other !is Id) return false

                    return instanceKeys == other.instanceKeys || timeStamp == other.timeStamp
                }
            }
        }
    }
}