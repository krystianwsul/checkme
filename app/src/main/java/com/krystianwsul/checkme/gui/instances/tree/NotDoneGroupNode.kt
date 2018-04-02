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
import junit.framework.Assert
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
        Assert.assertTrue(!instanceDatas.isEmpty())

        exactTimeStamp = instanceDatas.first()
                .InstanceTimeStamp
                .toExactTimeStamp()

        Assert.assertTrue(instanceDatas.all { it.InstanceTimeStamp.toExactTimeStamp() == exactTimeStamp })
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
            singleInstanceNodeCollection = NodeCollection(mDensity, mIndentation + 1, groupAdapter, false, treeNode, null)

            treeNode.setChildTreeNodes(singleInstanceNodeCollection!!.initialize(instanceDatas.single().children.values, expandedGroups, expandedInstances, doneExpanded, selectedNodes, selectable, null, false, null))
        } else {
            val notDoneInstanceTreeNodes = instanceDatas.map { newChildTreeNode(it, expandedInstances, selectedNodes) }

            treeNode.setChildTreeNodes(notDoneInstanceTreeNodes)
        }

        return treeNode
    }

    fun singleInstance(): Boolean {
        Assert.assertTrue(!instanceDatas.isEmpty())

        return instanceDatas.size == 1
    }

    fun addExpandedInstances(expandedInstances: Map<InstanceKey, Boolean>) {
        if (!expanded())
            return

        if (singleInstance()) {
            Assert.assertTrue(!expandedInstances.containsKey(singleInstanceData.InstanceKey))

            singleInstanceNodeCollection!!.addExpandedInstances(expandedInstances.toMutableMap().also {
                it[singleInstanceData.InstanceKey] = singleInstanceNodeCollection!!.doneExpanded
            })
        } else {
            notDoneInstanceNodes.forEach { it.addExpandedInstances(expandedInstances) }
        }
    }

    override fun getName(): Triple<String, Int, Boolean>? {
        return if (singleInstance()) {
            Triple(singleInstanceData.Name, ContextCompat.getColor(groupListFragment.activity!!, if (!singleInstanceData.TaskCurrent) R.color.textDisabled else R.color.textPrimary), true)
        } else {
            if (treeNode.isExpanded) {
                null
            } else {
                Triple(instanceDatas.sorted().joinToString(", ") { it.Name }, ContextCompat.getColor(groupListFragment.activity!!, R.color.textPrimary), true)
            }
        }
    }

    override fun getGroupAdapter() = nodeCollection.groupAdapter

    override fun getDetails(): Pair<String, Int>? {
        if (singleInstance()) {
            return if (singleInstanceData.DisplayText.isNullOrEmpty()) {
                null
            } else {
                Pair(singleInstanceData.DisplayText!!, ContextCompat.getColor(groupListFragment.activity!!, if (!singleInstanceData.TaskCurrent) R.color.textDisabled else R.color.textSecondary))
            }
        } else {
            val exactTimeStamp = (treeNode.modelNode as NotDoneGroupNode).exactTimeStamp

            val date = exactTimeStamp.date
            val hourMinute = exactTimeStamp.toTimeStamp().hourMinute

            val customTimeData = getCustomTimeData(date.dayOfWeek, hourMinute)

            val timeText = customTimeData?.Name ?: hourMinute.toString()

            val text = date.getDisplayText(groupListFragment.activity!!) + ", " + timeText

            return Pair(text, ContextCompat.getColor(groupListFragment.activity!!, R.color.textSecondary))
        }
    }

    override fun getChildren() = if (singleInstance()) {
        NotDoneInstanceNode.getChildrenNew(treeNode, singleInstanceData, groupListFragment)
    } else {
        null
    }

    override fun getExpand(): Pair<Int, View.OnClickListener>? {
        return if (singleInstance()) {
            val visibleChildren = treeNode.allChildren.any { it.canBeShown() }

            if (singleInstanceData.children.isEmpty() || groupListFragment.mSelectionCallback.hasActionMode() && (treeNode.hasSelectedDescendants() || !visibleChildren)) {
                null
            } else {
                Pair(if (treeNode.isExpanded) R.drawable.ic_expand_less_black_36dp else R.drawable.ic_expand_more_black_36dp, treeNode.expandListener)
            }
        } else {
            if (groupListFragment.mSelectionCallback.hasActionMode() && treeNode.hasSelectedDescendants()) {
                null
            } else {
                Pair(if (treeNode.isExpanded) R.drawable.ic_expand_less_black_36dp else R.drawable.ic_expand_more_black_36dp, treeNode.expandListener)
            }
        }
    }

    override fun getCheckBoxVisibility() = if (singleInstance()) {
        if (groupListFragment.mSelectionCallback.hasActionMode()) {
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

    override fun getCheckBoxChecked(): Boolean {
        Assert.assertTrue(singleInstance())
        Assert.assertTrue(!groupListFragment.mSelectionCallback.hasActionMode())

        return false
    }

    override fun getCheckBoxOnClickListener(): View.OnClickListener {
        val groupAdapter = nodeCollection.groupAdapter

        Assert.assertTrue(singleInstance())

        Assert.assertTrue(!groupAdapter.mGroupListFragment.mSelectionCallback.hasActionMode())

        return View.OnClickListener {
            it.setOnClickListener(null)

            singleInstanceData.Done = DomainFactory.getDomainFactory().setInstanceDone(groupAdapter.mGroupListFragment.activity!!, groupAdapter.mDataId, SaveService.Source.GUI, singleInstanceData.InstanceKey, true)
            Assert.assertTrue(singleInstanceData.Done != null)

            GroupListFragment.recursiveExists(singleInstanceData)

            nodeCollection.dividerNode.add(singleInstanceData)

            notDoneGroupCollection.remove(this)

            groupAdapter.mGroupListFragment.updateSelectAll()
        }
    }

    override fun getSeparatorVisibility() = if (this.treeNode.separatorVisibility) View.VISIBLE else View.INVISIBLE

    override fun getBackgroundColor(): Int {
        return if (singleInstance()) {
            if (treeNode.isSelected)
                ContextCompat.getColor(groupListFragment.activity!!, R.color.selected)
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
            if (groupListFragment.mDataWrapper?.TaskEditable != false && treeNode.isSelected && treeNodeCollection.selectedChildren.size == 1 && mIndentation == 0 && treeNodeCollection.nodes.none { it.isExpanded } && groupListFragment.mInstanceKeys == null && groupListFragment.taskKey == null) {
                check(singleInstance())

                groupListFragment.dragHelper.startDrag(viewHolder)
                true
            } else {
                treeNode.onLongClickListener.onLongClick(it)
            }
        }
    }

    override fun getOnClickListener() = treeNode.onClickListener

    override fun onClick() {
        groupListFragment.activity!!.startActivity(if (singleInstance()) {
            ShowInstanceActivity.getIntent(groupListFragment.activity!!, singleInstanceData.InstanceKey)
        } else {
            ShowGroupActivity.getIntent((treeNode.modelNode as NotDoneGroupNode).exactTimeStamp, groupListFragment.activity!!)
        })
    }

    private fun getCustomTimeData(dayOfWeek: DayOfWeek, hourMinute: HourMinute) = groupAdapter.mCustomTimeDatas.firstOrNull { it.HourMinutes[dayOfWeek] === hourMinute }

    private fun remove(notDoneInstanceNode: NotDoneInstanceNode) {
        Assert.assertTrue(instanceDatas.contains(notDoneInstanceNode.instanceData))
        instanceDatas.remove(notDoneInstanceNode.instanceData)

        Assert.assertTrue(notDoneInstanceNodes.contains(notDoneInstanceNode))
        notDoneInstanceNodes.remove(notDoneInstanceNode)

        val childTreeNode = notDoneInstanceNode.treeNode
        val selected = childTreeNode.isSelected

        if (selected)
            childTreeNode.deselect()

        treeNode.remove(childTreeNode)

        Assert.assertTrue(!instanceDatas.isEmpty())
        if (instanceDatas.size == 1) {
            val notDoneInstanceNode1 = notDoneInstanceNodes.single()

            val childTreeNode1 = notDoneInstanceNode1.treeNode

            notDoneInstanceNodes.remove(notDoneInstanceNode1)

            treeNode.remove(childTreeNode1)

            singleInstanceNodeCollection = NodeCollection(mDensity, mIndentation + 1, groupAdapter, false, treeNode, null)

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
                Assert.assertTrue(singleInstance())
                Assert.assertTrue(other.singleInstance())

                singleInstanceData.compareTo(other.singleInstanceData)
            }
        }
        is UnscheduledNode -> -1
        else -> {
            Assert.assertTrue(other is DividerNode)

            -1
        }
    }

    fun addInstanceData(instanceData: GroupListFragment.InstanceData) {
        Assert.assertTrue(instanceData.InstanceTimeStamp.toExactTimeStamp() == exactTimeStamp)

        Assert.assertTrue(!instanceDatas.isEmpty())
        if (instanceDatas.size == 1) {
            Assert.assertTrue(notDoneInstanceNodes.isEmpty())

            treeNode.removeAll()
            singleInstanceNodeCollection = null

            val instanceData1 = instanceDatas.single()

            val notDoneInstanceNode = NotDoneInstanceNode(mDensity, mIndentation, instanceData1, this@NotDoneGroupNode, selectable)
            notDoneInstanceNodes.add(notDoneInstanceNode)

            treeNode.add(notDoneInstanceNode.initialize(null, null, treeNode))
        }

        instanceDatas.add(instanceData)

        treeNode.add(newChildTreeNode(instanceData, null, null))
    }

    private fun newChildTreeNode(instanceData: GroupListFragment.InstanceData, expandedInstances: Map<InstanceKey, Boolean>?, selectedNodes: List<InstanceKey>?): TreeNode {
        val notDoneInstanceNode = NotDoneInstanceNode(mDensity, mIndentation, instanceData, this, selectable)

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

                DomainFactory.getDomainFactory().setTaskHierarchyOrdinal(groupListFragment.mDataId!!, it.hierarchyData)
            } else {
                it.ordinal = ordinal


                DomainFactory.getDomainFactory().setInstanceOrdinal(groupListFragment.mDataId!!, it.InstanceKey, ordinal)
            }
        }
    }

    class NotDoneInstanceNode(density: Float, indentation: Int, val instanceData: GroupListFragment.InstanceData, private val parentNotDoneGroupNode: NotDoneGroupNode, private val selectable: Boolean) : GroupHolderNode(density, indentation), ModelNode, NodeCollectionParent {

        companion object {

            fun getChildrenNew(treeNode: TreeNode, instanceData: GroupListFragment.InstanceData, groupListFragment: GroupListFragment) = instanceData.children
                    .values
                    .filter { it.Done == null }
                    .let {
                        fun color() = ContextCompat.getColor(groupListFragment.activity!!, if (!instanceData.TaskCurrent) {
                            R.color.textDisabled
                        } else {
                            R.color.textSecondary
                        })

                        if (it.isNotEmpty() && !treeNode.isExpanded) {
                            val children = it.sorted().joinToString(", ") { it.Name }

                            Pair(children, color())
                        } else if (!instanceData.mNote.isNullOrEmpty()) {
                            Pair(instanceData.mNote!!, color())
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

            nodeCollection = NodeCollection(mDensity, mIndentation + 1, groupAdapter, false, treeNode, null)
            treeNode.setChildTreeNodes(nodeCollection.initialize(instanceData.children.values, null, expandedInstances, doneExpanded, selectedNodes, selectable, null, false, null))

            return this.treeNode
        }

        private fun expanded() = treeNode.isExpanded

        fun addExpandedInstances(expandedInstances: Map<InstanceKey, Boolean>) {
            if (!expanded())
                return

            Assert.assertTrue(!expandedInstances.containsKey(instanceData.InstanceKey))

            nodeCollection.addExpandedInstances(expandedInstances.toMutableMap().also {
                it[instanceData.InstanceKey] = nodeCollection.doneExpanded
            })
        }

        override fun getGroupAdapter() = parentNotDoneGroupNode.groupAdapter

        override fun getName() = Triple(instanceData.Name, ContextCompat.getColor(groupListFragment.activity!!, if (!instanceData.TaskCurrent) R.color.textDisabled else R.color.textPrimary), true)

        override fun getChildren() = Companion.getChildrenNew(treeNode, instanceData, groupListFragment)

        override fun getExpand(): Pair<Int, View.OnClickListener>? {
            val visibleChildren = treeNode.allChildren.any { it.canBeShown() }

            return if (instanceData.children.isEmpty() || groupListFragment.mSelectionCallback.hasActionMode() && (treeNode.hasSelectedDescendants() || !visibleChildren)) {
                null
            } else {
                Pair(if (treeNode.isExpanded) R.drawable.ic_expand_less_black_36dp else R.drawable.ic_expand_more_black_36dp, treeNode.expandListener)
            }
        }

        override fun getCheckBoxVisibility() = if (groupListFragment.mSelectionCallback.hasActionMode()) {
            View.INVISIBLE
        } else {
            View.VISIBLE
        }

        override fun getCheckBoxChecked() = false

        override fun getCheckBoxOnClickListener(): View.OnClickListener {
            val notDoneGroupTreeNode = parentNotDoneGroupNode.treeNode
            Assert.assertTrue(notDoneGroupTreeNode.isExpanded)

            val groupAdapter = parentNodeCollection.groupAdapter
            Assert.assertTrue(!groupAdapter.mGroupListFragment.mSelectionCallback.hasActionMode())

            return View.OnClickListener {
                it.setOnClickListener(null)

                Assert.assertTrue(notDoneGroupTreeNode.isExpanded)

                instanceData.Done = DomainFactory.getDomainFactory().setInstanceDone(groupAdapter.mGroupListFragment.activity!!, groupAdapter.mDataId, SaveService.Source.GUI, instanceData.InstanceKey, true)
                Assert.assertTrue(instanceData.Done != null)

                GroupListFragment.recursiveExists(instanceData)

                parentNotDoneGroupNode.remove(this)

                parentNodeCollection.dividerNode.add(instanceData)

                groupAdapter.mGroupListFragment.updateSelectAll()
            }
        }

        override fun getSeparatorVisibility() = if (treeNode.separatorVisibility) View.VISIBLE else View.INVISIBLE

        override fun getBackgroundColor(): Int {
            Assert.assertTrue(parentNotDoneGroupNode.treeNode.isExpanded)

            return if (treeNode.isSelected)
                ContextCompat.getColor(groupListFragment.activity!!, R.color.selected)
            else
                Color.TRANSPARENT
        }

        override fun getOnLongClickListener(viewHolder: RecyclerView.ViewHolder) = treeNode.onLongClickListener

        override fun getOnClickListener() = treeNode.onClickListener

        override fun onClick() {
            groupListFragment.activity!!.startActivity(ShowInstanceActivity.getIntent(groupListFragment.activity!!, instanceData.InstanceKey))
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
