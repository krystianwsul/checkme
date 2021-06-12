package com.krystianwsul.checkme.gui.instances.tree

import android.os.Parcelable
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.domainmodel.GroupType
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
import com.krystianwsul.common.criteria.SearchCriteria
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
            ThumbnailDelegate(this),
            IndentationDelegate(this),
            MultiLineDelegate(this), // this one always has to be last, because it depends on layout changes from prev
        )
    }

    final override val rowsDelegate get() = contentDelegate.rowsDelegate

    final override val widthKey
        get() = MultiLineDelegate.WidthKey(
            indentation,
            checkBoxState != CheckBoxState.Gone,
            thumbnail != null,
            treeNode.expandVisible,
        )

    /**
     * Note: there is a difference between the node's ID and the contents' ID.  (So far.)  For example, this ID is used for
     * animations, which is why DoneInstanceNode has a different implementation: we don't want toggling done/not done to
     * animate as movement.
     */
    override val id: Any get() = contentDelegate.id

    final override val propagateSelection = contentDelegate.propagateSelection

    fun initialize(
        contentDelegateStates: Map<ContentDelegate.Id, ContentDelegate.State>,
        nodeContainer: NodeContainer<AbstractHolder>,
    ) = contentDelegate.initialize(contentDelegateStates, nodeContainer, this)

    final override fun onClick(holder: AbstractHolder) = contentDelegate.onClick(holder)

    sealed class ContentDelegate : ThumbnailModelNode, Sortable, CheckableModelNode, Matchable {

        abstract val bridge: GroupTypeFactory.Bridge

        abstract val directInstanceDatas: List<GroupListDataWrapper.InstanceData>
        abstract val allInstanceDatas: List<GroupListDataWrapper.InstanceData>

        protected abstract val indentation: Int

        protected abstract val groupAdapter: GroupListFragment.GroupAdapter
        protected val groupListFragment get() = groupAdapter.groupListFragment

        abstract val rowsDelegate: DetailsNode.ProjectRowsDelegate
        abstract val treeNode: TreeNode<AbstractHolder>

        abstract val id: Id

        abstract val propagateSelection: Boolean

        abstract val states: Map<Id, State>

        abstract val name: String

        abstract fun initialize(
            contentDelegateStates: Map<Id, State>,
            nodeContainer: NodeContainer<AbstractHolder>,
            modelNode: DetailsNode.Parent,
        ): TreeNode<AbstractHolder>

        abstract fun onClick(holder: AbstractHolder)

        class Instance(
            override val groupAdapter: GroupListFragment.GroupAdapter,
            override val bridge: GroupTypeFactory.SingleBridge,
            override val indentation: Int,
            showDetails: Boolean = true,
        ) : ContentDelegate() {

            constructor(
                groupAdapter: GroupListFragment.GroupAdapter,
                instanceData: GroupListDataWrapper.InstanceData,
                indentation: Int,
                showDetails: Boolean = true,
            ) : this(
                groupAdapter,
                GroupTypeFactory.SingleBridge(instanceData),
                indentation,
                showDetails,
            )

            val instanceData get() = bridge.instanceData

            override val directInstanceDatas = listOf(instanceData)
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
                    GroupType.GroupingMode.NONE,
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

                        treeNode.treeViewAdapter.ignoreNextScroll()

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

            override val propagateSelection = false

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

            override fun getMatchResult(search: SearchCriteria.Search) =
                ModelNode.MatchResult.fromBoolean(instanceData.matchesSearch(search))

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
            override val bridge: GroupTypeFactory.SingleParent,
            override val directInstanceDatas: List<GroupListDataWrapper.InstanceData>,
            override val indentation: Int,
            private val nodeCollection: NodeCollection,
            private val timeChildren: List<GroupTypeFactory.TimeChild>,
            override val id: Id,
            override val rowsDelegate: GroupRowsDelegate,
            private val indentCheckBox: Boolean,
            private val showGroupActivityParameters: ShowGroupActivity.Parameters,
        ) : ContentDelegate() {

            override val allInstanceDatas get() = notDoneNodes.flatMap { it.contentDelegate.directInstanceDatas }

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

                val nodePairs = timeChildren.map {
                    val contentDelegate = it.toContentDelegate(groupAdapter, indentation, nodeCollection)

                    val notDoneNode = if (contentDelegate is Instance) {
                        NotDoneInstanceNode(
                            contentDelegate.indentation,
                            contentDelegate.instanceData,
                            modelNode,
                            nodeCollection.groupAdapter,
                        )
                    } else {
                        NotDoneGroupNode(contentDelegate.indentation, nodeCollection, contentDelegate)
                    }

                    val childTreeNode = notDoneNode.initialize(contentDelegateStates, treeNode)

                    childTreeNode to notDoneNode
                }

                val (childTreeNodes, notDoneNodes) = nodePairs.unzip()
                treeNode.setChildTreeNodes(childTreeNodes)
                this.notDoneNodes = notDoneNodes

                return treeNode
            }

            override val thumbnail: ImageState? = null

            override val checkBoxState
                get() =
                    if (treeNode.isExpanded || !indentCheckBox) CheckBoxState.Gone else CheckBoxState.Invisible

            override val propagateSelection = true

            override val states: Map<ContentDelegate.Id, ContentDelegate.State>
                get() {
                    val myState = mapOf(id to State(treeNode.isSelected, treeNode.expansionState))

                    return myState + notDoneNodes.map { it.contentDelegate.states }.flatten()
                }

            override val name get() = bridge.name

            override fun onClick(holder: AbstractHolder) = groupListFragment.activity.let {
                it.startActivity(ShowGroupActivity.getIntent(it, showGroupActivityParameters))
            }

            override val sortable = false

            override fun getOrdinal(): Double = throw UnsupportedOperationException()
            override fun setOrdinal(ordinal: Double) = throw UnsupportedOperationException()

            // I don't really understand, or feel like understanding, why these three funs need to access allInstanceDatas
            override fun normalize() = allInstanceDatas.forEach { it.normalize() }

            override fun matchesFilterParams(filterParams: FilterCriteria.Full.FilterParams) =
                allInstanceDatas.any { it.matchesFilterParams(filterParams) }

            override fun getMatchResult(search: SearchCriteria.Search) =
                ModelNode.MatchResult.fromBoolean(allInstanceDatas.any { it.matchesSearch(search) })

            sealed class GroupRowsDelegate : DetailsNode.ProjectRowsDelegate(null, R.color.textSecondary) {

                protected abstract val groupAdapter: GroupListFragment.GroupAdapter

                private fun getCustomTimeData(dayOfWeek: DayOfWeek, hourMinute: HourMinute) =
                    groupAdapter.customTimeDatas.firstOrNull { it.hourMinutes[dayOfWeek] == hourMinute }

                protected fun getTimeRow(timeStamp: TimeStamp): MultiLineRow {
                    val date = timeStamp.date
                    val hourMinute = timeStamp.hourMinute

                    val timeText = getCustomTimeData(date.dayOfWeek, hourMinute)?.name ?: hourMinute.toString()

                    val text = date.getDisplayText() + ", " + timeText

                    return MultiLineRow.Visible(text, R.color.textSecondary)
                }

                protected fun getChildren(allChildren: List<TreeNode<*>>) =
                    allChildren.filter { it.modelNode is NotDoneNode && it.canBeShown() }
                        .map { it.modelNode as NotDoneNode }
                        .sorted()
                        .joinToString(", ") { it.contentDelegate.name }

                class Time(
                    override val groupAdapter: GroupListFragment.GroupAdapter,
                    private val timeStamp: TimeStamp,
                ) : GroupRowsDelegate() {

                    private val details by lazy { getTimeRow(timeStamp) }

                    override fun getRowsWithoutProject(
                        isExpanded: Boolean,
                        allChildren: List<TreeNode<*>>
                    ): List<MultiLineRow> {
                        val name = if (isExpanded)
                            MultiLineRow.Invisible
                        else
                            MultiLineRow.Visible(getChildren(allChildren))

                        return listOf(name, details)
                    }
                }

                class Project(
                    override val groupAdapter: GroupListFragment.GroupAdapter,
                    private val timeStamp: TimeStamp?,
                    projectName: String,
                ) : GroupRowsDelegate() {

                    private val name = MultiLineRow.Visible(projectName)

                    private val details by lazy { timeStamp?.let(::getTimeRow) }

                    override fun getRowsWithoutProject(
                        isExpanded: Boolean,
                        allChildren: List<TreeNode<*>>,
                    ): List<MultiLineRow> {
                        val children = if (isExpanded)
                            null
                        else
                            MultiLineRow.Visible(getChildren(allChildren), R.color.textSecondary)

                        return listOfNotNull(name, details, children)
                    }
                }
            }

            sealed interface Id : ContentDelegate.Id {

                @Parcelize
                class Time(private val timeStamp: TimeStamp, private val instanceKeys: Set<InstanceKey>) : Id {

                    override fun hashCode() = 1

                    override fun equals(other: Any?): Boolean {
                        if (other === this) return true

                        if (other !is Time) return false

                        // instanceKeys covers the scenario where all the instances' times are changes via "select all"
                        return instanceKeys == other.instanceKeys || timeStamp == other.timeStamp
                    }
                }

                @Parcelize
                class Project(
                    private val timeStamp: TimeStamp,
                    private val instanceKeys: Set<InstanceKey>,
                    private val projectKey: ProjectKey.Shared,
                ) : Id {

                    override fun hashCode() = 1

                    override fun equals(other: Any?): Boolean {
                        if (other === this) return true

                        if (other !is Project) return false

                        if (projectKey != other.projectKey) return false

                        // instanceKeys covers the scenario where all the instances' times are changes via "select all"
                        return instanceKeys == other.instanceKeys || timeStamp == other.timeStamp
                    }
                }
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