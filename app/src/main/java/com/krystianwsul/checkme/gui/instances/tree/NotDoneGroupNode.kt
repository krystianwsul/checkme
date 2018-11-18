package com.krystianwsul.checkme.gui.instances.tree

import android.graphics.Color
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.view.View
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.gui.instances.ShowGroupActivity
import com.krystianwsul.checkme.gui.instances.ShowInstanceActivity
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.utils.InstanceKey
import com.krystianwsul.checkme.utils.time.DayOfWeek
import com.krystianwsul.checkme.utils.time.ExactTimeStamp
import com.krystianwsul.checkme.utils.time.HourMinute
import com.krystianwsul.checkme.utils.time.TimeStamp
import com.krystianwsul.treeadapter.ModelNode
import com.krystianwsul.treeadapter.NodeContainer
import com.krystianwsul.treeadapter.TreeNode
import java.util.*

class NotDoneGroupNode(density: Float, indentation: Int, private val notDoneGroupCollection: NotDoneGroupCollection, private val instanceDatas: MutableList<GroupListFragment.InstanceData>, private val selectable: Boolean) : GroupHolderNode(density, indentation), ModelNode, NodeCollectionParent {

    lateinit var treeNode: TreeNode
        private set

    private val notDoneInstanceNodes = ArrayList<NotDoneInstanceNode>()

    private var singleInstanceNodeCollection: NodeCollection? = null

    val exactTimeStamp: ExactTimeStamp

    val singleInstanceData get() = instanceDatas.single()

    val nodeCollection get() = notDoneGroupCollection.nodeCollection

    private val groupListFragment get() = groupAdapter.mGroupListFragment

    init {
        check(!instanceDatas.isEmpty())

        exactTimeStamp = instanceDatas.first()
                .InstanceTimeStamp
                .toExactTimeStamp()

        check(instanceDatas.all { it.InstanceTimeStamp.toExactTimeStamp() == exactTimeStamp })
    }

    fun initialize(expandedGroups: List<TimeStamp>?, expandedInstances: Map<InstanceKey, Boolean>?, selectedNodes: List<InstanceKey>?, nodeContainer: NodeContainer): TreeNode {
        val expanded: Boolean
        val doneExpanded: Boolean
        if (instanceDatas.size == 1) {
            val instanceData = instanceDatas.single()

            if (expandedInstances != null && expandedInstances.containsKey(instanceData.InstanceKey) && !instanceData.children.isEmpty()) {
                expanded = true
                doneExpanded = expandedInstances[instanceData.InstanceKey]!!
            } else {
                expanded = false
                doneExpanded = false
            }
        } else {
            expanded = expandedGroups != null && expandedGroups.contains(exactTimeStamp.toTimeStamp())
            doneExpanded = false
        }

        val selected = (instanceDatas.size == 1) && (selectedNodes?.contains(instanceDatas.single().InstanceKey) == true)

        treeNode = TreeNode(this, nodeContainer, expanded, selected)

        if (instanceDatas.size == 1) {
            singleInstanceNodeCollection = NodeCollection(density, indentation + 1, groupAdapter, false, treeNode, null)

            treeNode.setChildTreeNodes(singleInstanceNodeCollection!!.initialize(instanceDatas.single().children.values, expandedGroups, expandedInstances, doneExpanded, selectedNodes, selectable, null, false, null))
        } else {
            val notDoneInstanceTreeNodes = instanceDatas.map { newChildTreeNode(it, expandedInstances, selectedNodes) }

            treeNode.setChildTreeNodes(notDoneInstanceTreeNodes)
        }

        return treeNode
    }

    fun singleInstance(): Boolean {
        check(!instanceDatas.isEmpty())

        return instanceDatas.size == 1
    }

    fun addExpandedInstances(expandedInstances: Map<InstanceKey, Boolean>) {
        if (!expanded())
            return

        if (singleInstance()) {
            check(!expandedInstances.containsKey(singleInstanceData.InstanceKey))

            singleInstanceNodeCollection!!.addExpandedInstances(expandedInstances.toMutableMap().also {
                it[singleInstanceData.InstanceKey] = singleInstanceNodeCollection!!.doneExpanded
            })
        } else {
            notDoneInstanceNodes.forEach { it.addExpandedInstances(expandedInstances) }
        }
    }

    override val name
        get(): Triple<String, Int, Boolean>? {
        return if (singleInstance()) {
            Triple(singleInstanceData.Name, ContextCompat.getColor(groupListFragment.activity, if (!singleInstanceData.TaskCurrent) R.color.textDisabled else R.color.textPrimary), true)
        } else {
            if (treeNode.isExpanded) {
                null
            } else {
                Triple(instanceDatas.sorted().joinToString(", ") { it.Name }, ContextCompat.getColor(groupListFragment.activity, R.color.textPrimary), true)
            }
        }
    }

    override val groupAdapter by lazy { nodeCollection.groupAdapter }

    override val details
        get(): Pair<String, Int>? {
        if (singleInstance()) {
            return if (singleInstanceData.DisplayText.isNullOrEmpty()) {
                null
            } else {
                Pair(singleInstanceData.DisplayText!!, ContextCompat.getColor(groupListFragment.activity, if (!singleInstanceData.TaskCurrent) R.color.textDisabled else R.color.textSecondary))
            }
        } else {
            val exactTimeStamp = (treeNode.modelNode as NotDoneGroupNode).exactTimeStamp

            val date = exactTimeStamp.date
            val hourMinute = exactTimeStamp.toTimeStamp().hourMinute

            val customTimeData = getCustomTimeData(date.dayOfWeek, hourMinute)

            val timeText = customTimeData?.Name ?: hourMinute.toString()

            val text = date.getDisplayText() + ", " + timeText

            return Pair(text, ContextCompat.getColor(groupListFragment.activity, R.color.textSecondary))
        }
    }

    override val children
        get() = if (singleInstance()) {
        NotDoneInstanceNode.getChildrenNew(treeNode, singleInstanceData, groupListFragment)
    } else {
        null
    }

    override val expand
        get(): Pair<Int, View.OnClickListener>? {
        return if (singleInstance()) {
            val visibleChildren = treeNode.allChildren.any { it.canBeShown() }

            if (singleInstanceData.children.isEmpty() || groupListFragment.selectionCallback.hasActionMode && (treeNode.hasSelectedDescendants() || !visibleChildren)) {
                null
            } else {
                Pair(if (treeNode.isExpanded) R.drawable.ic_expand_less_black_36dp else R.drawable.ic_expand_more_black_36dp, treeNode.expandListener)
            }
        } else {
            if (groupListFragment.selectionCallback.hasActionMode && treeNode.hasSelectedDescendants()) {
                null
            } else {
                Pair(if (treeNode.isExpanded) R.drawable.ic_expand_less_black_36dp else R.drawable.ic_expand_more_black_36dp, treeNode.expandListener)
            }
        }
    }

    override val checkBoxVisibility
        get() = if (singleInstance()) {
            if (groupListFragment.selectionCallback.hasActionMode) {
            View.INVISIBLE
        } else {
            View.VISIBLE
        }
    } else {
        if (treeNode.isExpanded) {
            View.GONE
        } else {
            View.INVISIBLE
        }
    }

    override val checkBoxChecked = false

    override val checkBoxOnClickListener
        get(): View.OnClickListener {
        val groupAdapter = nodeCollection.groupAdapter

            check(singleInstance())

            check(!groupAdapter.mGroupListFragment.selectionCallback.hasActionMode)

        return View.OnClickListener {
            it.setOnClickListener(null)

            singleInstanceData.Done = DomainFactory.getKotlinDomainFactory().setInstanceDone(groupAdapter.mDataId, SaveService.Source.GUI, singleInstanceData.InstanceKey, true)
            check(singleInstanceData.Done != null)

            GroupListFragment.recursiveExists(singleInstanceData)

            nodeCollection.dividerNode.add(singleInstanceData)

            notDoneGroupCollection.remove(this)

            groupAdapter.mGroupListFragment.updateSelectAll()
        }
    }

    override val separatorVisibility get() = if (this.treeNode.separatorVisibility) View.VISIBLE else View.INVISIBLE

    override val backgroundColor
        get(): Int {
        return if (singleInstance()) {
            if (treeNode.isSelected)
                ContextCompat.getColor(groupListFragment.activity, R.color.selected)
            else
                Color.TRANSPARENT
        } else {
            Color.TRANSPARENT
        }
    }

    override fun getOnLongClickListener(viewHolder: RecyclerView.ViewHolder): View.OnLongClickListener {
        val groupListFragment = groupAdapter.mGroupListFragment
        val treeNodeCollection = groupAdapter.treeNodeCollection

        return View.OnLongClickListener {
            if (groupListFragment.parameters.dataWrapper.TaskEditable != false && treeNode.isSelected && treeNodeCollection.selectedChildren.size == 1 && indentation == 0 && treeNodeCollection.nodes.none { it.isExpanded } && (groupListFragment.parameters !is GroupListFragment.Parameters.InstanceKeys) && (groupListFragment.parameters !is GroupListFragment.Parameters.TaskKey)) {
                check(singleInstance())

                groupListFragment.dragHelper.startDrag(viewHolder)
                true
            } else {
                treeNode.onLongClickListener.onLongClick(it)
            }
        }
    }

    override val onClickListener get() = treeNode.onClickListener

    override fun onClick() {
        groupListFragment.activity.startActivity(if (singleInstance()) {
            ShowInstanceActivity.getIntent(groupListFragment.activity, singleInstanceData.InstanceKey)
        } else {
            ShowGroupActivity.getIntent((treeNode.modelNode as NotDoneGroupNode).exactTimeStamp, groupListFragment.activity)
        })
    }

    private fun getCustomTimeData(dayOfWeek: DayOfWeek, hourMinute: HourMinute) = groupAdapter.mCustomTimeDatas.firstOrNull { it.HourMinutes[dayOfWeek] === hourMinute }

    private fun remove(notDoneInstanceNode: NotDoneInstanceNode) {
        check(instanceDatas.contains(notDoneInstanceNode.instanceData))
        instanceDatas.remove(notDoneInstanceNode.instanceData)

        check(notDoneInstanceNodes.contains(notDoneInstanceNode))
        notDoneInstanceNodes.remove(notDoneInstanceNode)

        val childTreeNode = notDoneInstanceNode.treeNode
        val selected = childTreeNode.isSelected

        if (selected)
            childTreeNode.deselect()

        treeNode.remove(childTreeNode)

        check(!instanceDatas.isEmpty())
        if (instanceDatas.size == 1) {
            val notDoneInstanceNode1 = notDoneInstanceNodes.single()

            val childTreeNode1 = notDoneInstanceNode1.treeNode

            notDoneInstanceNodes.remove(notDoneInstanceNode1)

            treeNode.remove(childTreeNode1)

            singleInstanceNodeCollection = NodeCollection(density, indentation + 1, groupAdapter, false, treeNode, null)

            val childTreeNodes = singleInstanceNodeCollection!!.initialize(instanceDatas[0].children.values, null, null, false, null, selectable, null, false, null)

            childTreeNodes.forEach { treeNode.add(it) }

            if (selected)
                this.treeNode.select()
        }
    }

    override fun compareTo(other: ModelNode) = when (other) {
        is NoteNode -> 1
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
        is UnscheduledNode -> -1
        else -> {
            check(other is DividerNode)

            -1
        }
    }

    fun addInstanceData(instanceData: GroupListFragment.InstanceData) {
        check(instanceData.InstanceTimeStamp.toExactTimeStamp() == exactTimeStamp)

        check(!instanceDatas.isEmpty())
        if (instanceDatas.size == 1) {
            check(notDoneInstanceNodes.isEmpty())

            treeNode.removeAll()
            singleInstanceNodeCollection = null

            val instanceData1 = instanceDatas.single()

            val notDoneInstanceNode = NotDoneInstanceNode(density, indentation, instanceData1, this@NotDoneGroupNode, selectable)
            notDoneInstanceNodes.add(notDoneInstanceNode)

            treeNode.add(notDoneInstanceNode.initialize(null, null, treeNode))
        }

        instanceDatas.add(instanceData)

        treeNode.add(newChildTreeNode(instanceData, null, null))
    }

    private fun newChildTreeNode(instanceData: GroupListFragment.InstanceData, expandedInstances: Map<InstanceKey, Boolean>?, selectedNodes: List<InstanceKey>?): TreeNode {
        val notDoneInstanceNode = NotDoneInstanceNode(density, indentation, instanceData, this, selectable)

        val childTreeNode = notDoneInstanceNode.initialize(expandedInstances, selectedNodes, treeNode)

        notDoneInstanceNodes.add(notDoneInstanceNode)

        return childTreeNode
    }

    fun expanded() = treeNode.isExpanded

    override val isSelectable get() = selectable && notDoneInstanceNodes.isEmpty()

    override val isVisibleWhenEmpty = true

    override val isVisibleDuringActionMode = true

    override val isSeparatorVisibleWhenNotExpanded = false

    fun removeFromParent() {
        notDoneGroupCollection.remove(this)
    }

    override fun getOrdinal() = singleInstanceData.run { hierarchyData?.ordinal ?: ordinal }

    override fun setOrdinal(ordinal: Double) {
        singleInstanceData.let {
            if (it.hierarchyData != null) {
                it.hierarchyData.ordinal = ordinal

                DomainFactory.getKotlinDomainFactory().setTaskHierarchyOrdinal(groupListFragment.parameters.dataId, it.hierarchyData)
            } else {
                it.ordinal = ordinal

                DomainFactory.getKotlinDomainFactory().setInstanceOrdinal(groupListFragment.parameters.dataId, it.InstanceKey, ordinal)
            }
        }
    }

    class NotDoneInstanceNode(density: Float, indentation: Int, val instanceData: GroupListFragment.InstanceData, private val parentNotDoneGroupNode: NotDoneGroupNode, private val selectable: Boolean) : GroupHolderNode(density, indentation), ModelNode, NodeCollectionParent {

        companion object {

            fun getChildrenNew(treeNode: TreeNode, instanceData: GroupListFragment.InstanceData, groupListFragment: GroupListFragment) = instanceData.children
                    .values
                    .filter { it.Done == null }
                    .let {
                        fun color() = ContextCompat.getColor(groupListFragment.activity, if (!instanceData.TaskCurrent) {
                            R.color.textDisabled
                        } else {
                            R.color.textSecondary
                        })

                        if (it.isNotEmpty() && !treeNode.isExpanded) {
                            val children = it.sorted().joinToString(", ") { it.Name }

                            Pair(children, color())
                        } else if (!instanceData.mNote.isNullOrEmpty()) {
                            Pair(instanceData.mNote, color())
                        } else {
                            null
                        }
            }
        }

        lateinit var treeNode: TreeNode
            private set

        private lateinit var nodeCollection: NodeCollection

        private val parentNotDoneGroupCollection get() = parentNotDoneGroupNode.notDoneGroupCollection

        val parentNodeCollection get() = parentNotDoneGroupCollection.nodeCollection

        private val groupListFragment get() = groupAdapter.mGroupListFragment

        fun initialize(expandedInstances: Map<InstanceKey, Boolean>?, selectedNodes: List<InstanceKey>?, notDoneGroupTreeNode: TreeNode): TreeNode {
            val selected = selectedNodes?.contains(instanceData.InstanceKey) == true

            val expanded: Boolean
            val doneExpanded: Boolean
            if (expandedInstances?.containsKey(instanceData.InstanceKey) == true && !instanceData.children.isEmpty()) {
                expanded = true
                doneExpanded = expandedInstances[instanceData.InstanceKey]!!
            } else {
                expanded = false
                doneExpanded = false
            }

            treeNode = TreeNode(this, notDoneGroupTreeNode, expanded, selected)

            nodeCollection = NodeCollection(density, indentation + 1, groupAdapter, false, treeNode, null)
            treeNode.setChildTreeNodes(nodeCollection.initialize(instanceData.children.values, null, expandedInstances, doneExpanded, selectedNodes, selectable, null, false, null))

            return this.treeNode
        }

        private fun expanded() = treeNode.isExpanded

        fun addExpandedInstances(expandedInstances: Map<InstanceKey, Boolean>) {
            if (!expanded())
                return

            check(!expandedInstances.containsKey(instanceData.InstanceKey))

            nodeCollection.addExpandedInstances(expandedInstances.toMutableMap().also {
                it[instanceData.InstanceKey] = nodeCollection.doneExpanded
            })
        }

        override val groupAdapter by lazy { parentNotDoneGroupNode.groupAdapter }

        override val name get() = Triple(instanceData.Name, ContextCompat.getColor(groupListFragment.activity, if (!instanceData.TaskCurrent) R.color.textDisabled else R.color.textPrimary), true)

        override val children get() = getChildrenNew(treeNode, instanceData, groupListFragment)

        override val expand
            get(): Pair<Int, View.OnClickListener>? {
            val visibleChildren = treeNode.allChildren.any { it.canBeShown() }

                return if (instanceData.children.isEmpty() || groupListFragment.selectionCallback.hasActionMode && (treeNode.hasSelectedDescendants() || !visibleChildren)) {
                null
            } else {
                Pair(if (treeNode.isExpanded) R.drawable.ic_expand_less_black_36dp else R.drawable.ic_expand_more_black_36dp, treeNode.expandListener)
            }
        }

        override val checkBoxVisibility
            get() = if (groupListFragment.selectionCallback.hasActionMode) {
            View.INVISIBLE
        } else {
            View.VISIBLE
        }

        override val checkBoxChecked = false

        override val checkBoxOnClickListener
            get(): View.OnClickListener {
            val notDoneGroupTreeNode = parentNotDoneGroupNode.treeNode
                check(notDoneGroupTreeNode.isExpanded)

            val groupAdapter = parentNodeCollection.groupAdapter
                check(!groupAdapter.mGroupListFragment.selectionCallback.hasActionMode)

            return View.OnClickListener {
                it.setOnClickListener(null)

                check(notDoneGroupTreeNode.isExpanded)

                instanceData.Done = DomainFactory.getKotlinDomainFactory().setInstanceDone(groupAdapter.mDataId, SaveService.Source.GUI, instanceData.InstanceKey, true)
                check(instanceData.Done != null)

                GroupListFragment.recursiveExists(instanceData)

                parentNotDoneGroupNode.remove(this)

                parentNodeCollection.dividerNode.add(instanceData)

                groupAdapter.mGroupListFragment.updateSelectAll()
            }
        }

        override val separatorVisibility get() = if (treeNode.separatorVisibility) View.VISIBLE else View.INVISIBLE

        override val backgroundColor
            get(): Int {
                check(parentNotDoneGroupNode.treeNode.isExpanded)

            return if (treeNode.isSelected)
                ContextCompat.getColor(groupListFragment.activity, R.color.selected)
            else
                Color.TRANSPARENT
        }

        override fun getOnLongClickListener(viewHolder: RecyclerView.ViewHolder) = treeNode.onLongClickListener

        override val onClickListener get() = treeNode.onClickListener

        override fun onClick() {
            groupListFragment.activity.startActivity(ShowInstanceActivity.getIntent(groupListFragment.activity, instanceData.InstanceKey))
        }

        override fun compareTo(other: ModelNode) = instanceData.compareTo((other as NotDoneInstanceNode).instanceData)

        override val isSelectable = selectable

        override val isVisibleWhenEmpty = true

        override val isVisibleDuringActionMode = true

        override val isSeparatorVisibleWhenNotExpanded = false

        fun removeFromParent() {
            parentNotDoneGroupNode.remove(this)
        }
    }
}
