package com.krystianwsul.checkme.gui.instances.tree

import android.os.Parcelable
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
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.treeadapter.*
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.kotlin.addTo
import kotlinx.parcelize.Parcelize

sealed class NotDoneNode(val contentDelegate: ContentDelegate) :
    AbstractModelNode(),
    NodeCollectionParent,
    Sortable by contentDelegate,
    CheckableModelNode by contentDelegate,
    MultiLineModelNode,
    ThumbnailModelNode by contentDelegate,
    IndentationModelNode,
    DetailsNode.Parent,
    Matchable by contentDelegate {

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

    final override val widthKey
        get() = MultiLineDelegate.WidthKey(
            indentation,
            checkBoxState != CheckBoxState.Gone,
            thumbnail != null,
            true,
        )

    /**
     * Note: there is a difference between the node's ID and the contents' ID.  (So far.)  For example, this ID is used for
     * animations, which is why DoneInstanceNode has a different implementation: we don't want toggling done/not done to
     * animate as movement.
     */
    override val id: Any get() = contentDelegate.id

    final override val toggleDescendants = contentDelegate.toggleDescendants

    fun initialize(
        contentDelegateStates: Map<ContentDelegate.Id, ContentDelegate.State>,
        nodeContainer: NodeContainer<AbstractHolder>,
    ) = contentDelegate.initialize(contentDelegateStates, nodeContainer, this)

    final override fun onClick(holder: AbstractHolder) = contentDelegate.onClick(holder)

    sealed class ContentDelegate : ThumbnailModelNode, Sortable, CheckableModelNode, Comparable<ContentDelegate>, Matchable {

        abstract val directInstanceDatas: List<GroupListDataWrapper.InstanceData>
        abstract val firstInstanceData: GroupListDataWrapper.InstanceData
        abstract val allInstanceDatas: List<GroupListDataWrapper.InstanceData>

        protected abstract val groupAdapter: GroupListFragment.GroupAdapter
        protected val groupListFragment get() = groupAdapter.groupListFragment

        abstract val rowsDelegate: DetailsNode.ProjectRowsDelegate
        abstract val treeNode: TreeNode<AbstractHolder>
        abstract val id: Id
        abstract val toggleDescendants: Boolean

        abstract val states: Map<Id, State>

        abstract val name: String

        abstract fun initialize(
            contentDelegateStates: Map<Id, State>,
            nodeContainer: NodeContainer<AbstractHolder>,
            modelNode: DetailsNode.Parent,
        ): TreeNode<AbstractHolder>

        abstract fun onClick(holder: AbstractHolder)

        override fun compareTo(other: ContentDelegate): Int {
            return firstInstanceData.compareTo(other.firstInstanceData)
        }

        class Instance(
            override val groupAdapter: GroupListFragment.GroupAdapter,
            val instanceData: GroupListDataWrapper.InstanceData,
            private val indentation: Int,
            showDetails: Boolean = true,
        ) : ContentDelegate() {

            override val directInstanceDatas = listOf(instanceData)
            override val firstInstanceData = instanceData
            override val allInstanceDatas = directInstanceDatas

            override lateinit var treeNode: TreeNode<AbstractHolder>
            private lateinit var nodeCollection: NodeCollection

            override val name = instanceData.name

            override fun initialize(
                contentDelegateStates: Map<ContentDelegate.Id, ContentDelegate.State>,
                nodeContainer: NodeContainer<AbstractHolder>,
                modelNode: DetailsNode.Parent,
            ): TreeNode<AbstractHolder> {
                val state = contentDelegateStates[id] as? State ?: State()
                val (expansionState, doneExpansionState) = state.collectionExpansionState

                treeNode = TreeNode(modelNode, nodeContainer, state.selected, expansionState)

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
                        contentDelegateStates,
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

            override val id: ContentDelegate.Id by lazy { Id(instanceData.instanceKey) }

            override val toggleDescendants = false

            override val states: Map<ContentDelegate.Id, ContentDelegate.State>
                get() {
                    val collectionExpansionState = CollectionExpansionState(
                        treeNode.expansionState,
                        nodeCollection.doneExpansionState,
                    )

                    val myState = mapOf(id to State(treeNode.isSelected, collectionExpansionState))

                    return myState + nodeCollection.contentDelegateStates
                }

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

            @Parcelize
            private data class Id(val instanceKey: InstanceKey) : ContentDelegate.Id

            @Parcelize
            private data class State(
                val selected: Boolean,
                val collectionExpansionState: CollectionExpansionState,
            ) : ContentDelegate.State {

                constructor() : this(false, CollectionExpansionState())
            }
        }

        class Group(
            override val groupAdapter: GroupListFragment.GroupAdapter,
            private val groupType: GroupType,
            override val directInstanceDatas: List<GroupListDataWrapper.InstanceData>,
            override val firstInstanceData: GroupListDataWrapper.InstanceData,
            private val indentation: Int,
            private val nodeCollection: NodeCollection,
            private val childGroupTypes: List<GroupType>,
            override val id: Id,
        ) : ContentDelegate() {

            override val allInstanceDatas get() = notDoneNodes.flatMap { it.contentDelegate.directInstanceDatas }

            private val timeStamp by lazy { // todo project later
                allInstanceDatas.map { it.instanceTimeStamp }
                    .distinct()
                    .single()
            }

            override val rowsDelegate: DetailsNode.ProjectRowsDelegate by lazy { TimeRowsDelegate(groupAdapter, timeStamp) }

            override lateinit var treeNode: TreeNode<AbstractHolder>
            private lateinit var notDoneNodes: List<NotDoneNode>

            override fun initialize(
                contentDelegateStates: Map<ContentDelegate.Id, ContentDelegate.State>,
                nodeContainer: NodeContainer<AbstractHolder>,
                modelNode: DetailsNode.Parent,
            ): TreeNode<AbstractHolder> {
                val state = contentDelegateStates[id] as? State ?: State()

                treeNode = TreeNode(
                    modelNode,
                    nodeContainer,
                    state.selected,
                    state.expansionState,
                )

                val nodePairs = if (true) {
                    childGroupTypes.map { it.toContentDelegate(groupAdapter, indentation, nodeCollection) }.map {
                        // todo project not sure about the whole group/instanceNode thing
                        val notDoneNode = if (it is Instance) {
                            NotDoneInstanceNode(indentation, it.instanceData, modelNode, nodeCollection.groupAdapter)
                        } else {
                            NotDoneGroupNode(indentation, nodeCollection, it)
                        }

                        val childTreeNode = notDoneNode.initialize(contentDelegateStates, treeNode)

                        childTreeNode to notDoneNode
                    }
                } else {
                    groupType.instanceDatas.map { // todo project InstanceDatas
                        val notDoneInstanceNode = NotDoneInstanceNode(indentation, it, modelNode, groupAdapter)

                        val childTreeNode = notDoneInstanceNode.initialize(contentDelegateStates, treeNode)

                        childTreeNode to notDoneInstanceNode
                    }
                }

                val (childTreeNodes, notDoneNodes) = nodePairs.unzip()
                treeNode.setChildTreeNodes(childTreeNodes)
                this.notDoneNodes = notDoneNodes

                return treeNode
            }

            override val thumbnail: ImageState? = null

            override val checkBoxState get() = if (treeNode.isExpanded) CheckBoxState.Gone else CheckBoxState.Invisible

            override val toggleDescendants = true

            override val states: Map<ContentDelegate.Id, ContentDelegate.State>
                get() {
                    val myState = mapOf(id to State(treeNode.isSelected, treeNode.expansionState))

                    return myState + notDoneNodes.map { it.contentDelegate.states }.flatten()
                }

            override val name get() = groupType.name

            override fun onClick(holder: AbstractHolder) =
                groupListFragment.activity.let { it.startActivity(ShowGroupActivity.getIntent(timeStamp, it)) }

            override fun getOrdinal(): Double = throw UnsupportedOperationException()
            override fun setOrdinal(ordinal: Double) = throw UnsupportedOperationException()

            // I don't really understand, or feel like understanding, why these three funs need to access allInstanceDatas
            override fun normalize() = allInstanceDatas.forEach { it.normalize() }

            override fun matchesFilterParams(filterParams: FilterCriteria.Full.FilterParams) =
                allInstanceDatas.any { it.matchesFilterParams(filterParams) }

            override fun getMatchResult(query: String) =
                ModelNode.MatchResult.fromBoolean(allInstanceDatas.any { it.matchesQuery(query) })

            class TimeRowsDelegate(
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
                            allChildren.filter { it.modelNode is NotDoneNode && it.canBeShown() }
                                .map { it.modelNode as NotDoneNode }
                                .sorted()
                                .joinToString(", ") { it.contentDelegate.name }
                        )
                    }

                    return listOf(name, details)
                }
            }

            sealed interface Id : ContentDelegate.Id {

                @Parcelize
                data class Time(val timeStamp: TimeStamp) : Id

                @Parcelize
                data class Project(val timeStamp: TimeStamp, val projectKey: ProjectKey.Shared) : Id
            }

            @Parcelize
            private data class State(
                val selected: Boolean,
                val expansionState: TreeNode.ExpansionState,
            ) : ContentDelegate.State {

                constructor() : this(false, TreeNode.ExpansionState())
            }
        }

        sealed interface Id : Parcelable

        sealed interface State : Parcelable
    }
}