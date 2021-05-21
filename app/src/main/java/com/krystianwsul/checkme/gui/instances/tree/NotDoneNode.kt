package com.krystianwsul.checkme.gui.instances.tree

import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.gui.instances.ShowGroupActivity
import com.krystianwsul.checkme.gui.instances.ShowInstanceActivity
import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.checkme.gui.instances.list.GroupListFragment
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
import com.krystianwsul.checkme.gui.tree.delegates.multiline.MultiLineRow
import com.krystianwsul.checkme.gui.tree.delegates.thumbnail.ThumbnailDelegate
import com.krystianwsul.checkme.gui.tree.delegates.thumbnail.ThumbnailModelNode
import com.krystianwsul.checkme.gui.utils.flatten
import com.krystianwsul.checkme.utils.time.getDisplayText
import com.krystianwsul.common.firebase.models.ImageState
import com.krystianwsul.common.time.DayOfWeek
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.HourMinute
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

    final override val holderType = HolderType.CHECKABLE

    final override val isSelectable = true

    final override val isDraggable = true

    abstract override val treeNode: TreeNode<AbstractHolder>

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

    final override val thumbnail get() = contentDelegate.thumbnail

    val instanceExpansionStates get() = contentDelegate.instanceExpansionStates

    final override fun onClick(holder: AbstractHolder) = contentDelegate.onClick(holder)

    protected sealed class ContentDelegate {

        protected abstract val groupAdapter: GroupListFragment.GroupAdapter
        protected val groupListFragment get() = groupAdapter.groupListFragment

        abstract val rowsDelegate: DetailsNode.ProjectRowsDelegate
        abstract val instanceExpansionStates: Map<InstanceKey, CollectionExpansionState>
        abstract val thumbnail: ImageState?

        abstract fun onClick(holder: AbstractHolder)

        class Instance(private val instanceData: GroupListDataWrapper.InstanceData) : ContentDelegate() {

            override lateinit var groupAdapter: GroupListFragment.GroupAdapter
            private lateinit var treeNode: TreeNode<*>
            private lateinit var nodeCollection: NodeCollection

            override val rowsDelegate = InstanceRowsDelegate(instanceData)

            fun initialize(
                groupAdapter: GroupListFragment.GroupAdapter,
                treeNode: TreeNode<*>,
                nodeCollection: NodeCollection,
            ) {
                this.groupAdapter = groupAdapter
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

            override val thumbnail = instanceData.imageState

            override fun onClick(holder: AbstractHolder) = groupListFragment.activity.let {
                it.startActivity(ShowInstanceActivity.getIntent(it, instanceData.instanceKey))
            }
        }

        class Group(
            override val groupAdapter: GroupListFragment.GroupAdapter,
            instanceDatas: List<GroupListDataWrapper.InstanceData>,
        ) : ContentDelegate() {

            init {
                check(instanceDatas.size > 1)
            }

            val exactTimeStamp = instanceDatas.map { it.instanceTimeStamp }
                .distinct()
                .single()
                .toLocalExactTimeStamp()

            override val rowsDelegate: DetailsNode.ProjectRowsDelegate =
                GroupRowsDelegate(groupAdapter, exactTimeStamp)

            private lateinit var notDoneInstanceNodes: List<NotDoneInstanceNode>

            fun initialize(notDoneInstanceNodes: List<NotDoneInstanceNode>) {
                this.notDoneInstanceNodes = notDoneInstanceNodes
            }

            override val instanceExpansionStates get() = notDoneInstanceNodes.map { it.instanceExpansionStates }.flatten()

            override val thumbnail: ImageState? = null

            override fun onClick(holder: AbstractHolder) =
                groupListFragment.activity.let { it.startActivity(ShowGroupActivity.getIntent(exactTimeStamp, it)) }

            private class GroupRowsDelegate(
                private val groupAdapter: GroupListFragment.GroupAdapter,
                private val exactTimeStamp: ExactTimeStamp.Local,
            ) : DetailsNode.ProjectRowsDelegate(null, R.color.textSecondary) {

                private fun getCustomTimeData(dayOfWeek: DayOfWeek, hourMinute: HourMinute) =
                    groupAdapter.customTimeDatas.firstOrNull { it.hourMinutes[dayOfWeek] == hourMinute }

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
        }
    }
}