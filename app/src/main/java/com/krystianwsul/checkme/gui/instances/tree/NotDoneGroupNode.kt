package com.krystianwsul.checkme.gui.instances.tree

import androidx.recyclerview.widget.RecyclerView
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.gui.instances.ShowGroupActivity
import com.krystianwsul.checkme.gui.instances.ShowInstanceActivity
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.utils.time.getDisplayText
import com.krystianwsul.common.time.DayOfWeek
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.HourMinute
import com.krystianwsul.common.time.TimeStamp
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.treeadapter.*
import java.util.*

class NotDoneGroupNode(
        indentation: Int,
        private val notDoneGroupCollection: NotDoneGroupCollection,
        val instanceDatas: MutableList<GroupListFragment.InstanceData>
) : GroupHolderNode(indentation), NodeCollectionParent, Sortable {

    public override lateinit var treeNode: TreeNode<NodeHolder>
        private set

    override val ripple = true

    private val notDoneInstanceNodes = ArrayList<NotDoneInstanceNode>()

    private var singleInstanceNodeCollection: NodeCollection? = null

    val exactTimeStamp: ExactTimeStamp

    val singleInstanceData get() = instanceDatas.single()

    val nodeCollection get() = notDoneGroupCollection.nodeCollection

    private val groupListFragment get() = groupAdapter.groupListFragment

    override val thumbnail get() = if (singleInstance()) singleInstanceData.imageState else null

    init {
        check(instanceDatas.isNotEmpty())

        exactTimeStamp = instanceDatas.map { it.instanceTimeStamp }
                .distinct()
                .single()
                .toExactTimeStamp()

        check(instanceDatas.all { it.instanceTimeStamp.toExactTimeStamp() == exactTimeStamp })
    }

    fun initialize(
            expandedGroups: List<TimeStamp>,
            expandedInstances: Map<InstanceKey, Boolean>,
            selectedInstances: List<InstanceKey>,
            selectedGroups: List<Long>,
            nodeContainer: NodeContainer<NodeHolder>
    ): TreeNode<NodeHolder> {
        check(instanceDatas.isNotEmpty())

        val instanceData = instanceDatas.singleOrNull()

        val (expanded, doneExpanded) = instanceData?.run {
            if (expandedInstances.containsKey(instanceKey) && children.isNotEmpty())
                true to expandedInstances.getValue(instanceKey)
            else
                false to false
        } ?: expandedGroups.contains(exactTimeStamp.toTimeStamp()) to false

        val selected = instanceData?.let {
            selectedInstances.contains(it.instanceKey)
        } ?: selectedGroups.contains(exactTimeStamp.long)

        treeNode = TreeNode(this, nodeContainer, expanded, selected)

        if (instanceData != null) {
            singleInstanceNodeCollection = NodeCollection(indentation + 1, groupAdapter, false, treeNode, null)

            treeNode.setChildTreeNodes(singleInstanceNodeCollection!!.initialize(
                    instanceData.children.values,
                    expandedGroups,
                    expandedInstances,
                    doneExpanded,
                    selectedInstances,
                    selectedGroups,
                    listOf(),
                    false,
                    listOf(),
                    listOf(),
                    null
            ))
        } else {
            treeNode.setChildTreeNodes(
                    instanceDatas.map {
                        newChildTreeNode(
                                it,
                                expandedInstances,
                                selectedInstances.contains(it.instanceKey),
                                selectedInstances,
                                selectedGroups
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

    fun addExpandedInstances(expandedInstances: MutableMap<InstanceKey, Boolean>) {
        if (!expanded())
            return

        if (singleInstance()) {
            check(!expandedInstances.containsKey(singleInstanceData.instanceKey))

            expandedInstances[singleInstanceData.instanceKey] = singleInstanceNodeCollection!!.doneExpanded
            singleInstanceNodeCollection!!.addExpandedInstances(expandedInstances)
        } else {
            notDoneInstanceNodes.forEach { it.addExpandedInstances(expandedInstances) }
        }
    }

    override val name
        get() = if (singleInstance()) {
            NameData(singleInstanceData.name, if (!singleInstanceData.taskCurrent) colorDisabled else colorPrimary)
        } else {
            if (treeNode.isExpanded) {
                null
            } else {
                NameData(instanceDatas.sorted().joinToString(", ") { it.name })
            }
        }

    override val groupAdapter by lazy { nodeCollection.groupAdapter }

    override val details
        get() = if (singleInstance()) {
            if (singleInstanceData.displayText.isNullOrEmpty()) {
                null
            } else {
                Pair(
                        singleInstanceData.displayText!!,
                        if (!singleInstanceData.taskCurrent) colorDisabled else colorSecondary
                )
            }
        } else {
            val exactTimeStamp = (treeNode.modelNode as NotDoneGroupNode).exactTimeStamp

            val date = exactTimeStamp.date
            val hourMinute = exactTimeStamp.toTimeStamp().hourMinute

            val timeText = getCustomTimeData(date.dayOfWeek, hourMinute)?.Name
                    ?: hourMinute.toString()

            val text = date.getDisplayText() + ", " + timeText

            Pair(text, colorSecondary)
        }

    override val children
        get() = if (singleInstance()) {
            NotDoneInstanceNode.getChildrenNew(treeNode, singleInstanceData)
        } else {
            null
        }

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

                    val newDone = singleInstanceData.done == null

                    fun updateDone() = DomainFactory.instance.setInstanceDone(
                            groupAdapter.dataId,
                            SaveService.Source.GUI,
                            instanceKey,
                            newDone
                    )

                    if (nodeCollection.useDoneNode) {
                        check(newDone)

                        groupAdapter.treeNodeCollection
                                .treeViewAdapter
                                .updateDisplayedNodes {
                                    singleInstanceData.done = updateDone()!!

                                    GroupListFragment.recursiveExists(singleInstanceData)

                                    nodeCollection.dividerNode.add(singleInstanceData, TreeViewAdapter.Placeholder)

                                    notDoneGroupCollection.remove(this, TreeViewAdapter.Placeholder)
                                }
                    } else {
                        updateDone()
                    }

                    groupListFragment.listener.showSnackbarDone(1) {
                        DomainFactory.instance.setInstanceDone(
                                0,
                                SaveService.Source.GUI,
                                instanceKey,
                                !newDone
                        )
                    }
                }
            }
        } else {
            if (treeNode.isExpanded)
                CheckBoxState.Gone
            else
                CheckBoxState.Invisible
        }

    override fun onLongClick(viewHolder: RecyclerView.ViewHolder) {
        val groupListFragment = groupAdapter.groupListFragment
        val treeNodeCollection = groupAdapter.treeNodeCollection

        if (singleInstance() &&
                groupListFragment.parameters.dataWrapper.taskEditable != false &&
                treeNodeCollection.selectedChildren.isEmpty() &&
                indentation == 0 &&
                treeNodeCollection.nodes.none { it.isExpanded } &&
                groupListFragment.parameters.draggable
        ) {
            groupListFragment.dragHelper.startDrag(viewHolder)
            treeNode.onLongClickSelect(viewHolder, true)
        } else {
            treeNode.onLongClickSelect(viewHolder, false)
        }
    }

    override fun onClick(holder: NodeHolder) {
        groupListFragment.activity.startActivity(if (singleInstance()) {
            ShowInstanceActivity.getIntent(groupListFragment.activity, singleInstanceData.instanceKey)
        } else {
            ShowGroupActivity.getIntent((treeNode.modelNode as NotDoneGroupNode).exactTimeStamp, groupListFragment.activity)
        })
    }

    private fun getCustomTimeData(dayOfWeek: DayOfWeek, hourMinute: HourMinute) = groupAdapter.customTimeDatas.firstOrNull { it.HourMinutes[dayOfWeek] === hourMinute }

    private fun remove(notDoneInstanceNode: NotDoneInstanceNode, x: TreeViewAdapter.Placeholder) {
        check(instanceDatas.contains(notDoneInstanceNode.instanceData))
        instanceDatas.remove(notDoneInstanceNode.instanceData)

        check(notDoneInstanceNodes.contains(notDoneInstanceNode))
        notDoneInstanceNodes.remove(notDoneInstanceNode)

        val childTreeNode = notDoneInstanceNode.treeNode
        val selected = childTreeNode.isSelected

        if (selected)
            childTreeNode.deselect(x, false)

        treeNode.remove(childTreeNode, x)

        check(instanceDatas.isNotEmpty())
        if (instanceDatas.size == 1) {
            val notDoneInstanceNode1 = notDoneInstanceNodes.single()
            notDoneInstanceNodes.remove(notDoneInstanceNode1)

            val childTreeNode1 = notDoneInstanceNode1.treeNode
            val selected1 = childTreeNode1.isSelected

            if (selected1) {
                if (!treeNode.isSelected)
                    treeNode.select(x, false)

                childTreeNode1.deselect(x, false)
            }

            treeNode.remove(childTreeNode1, x)

            singleInstanceNodeCollection = NodeCollection(indentation + 1, groupAdapter, false, treeNode, null)

            val childTreeNodes = singleInstanceNodeCollection!!.initialize(
                    instanceDatas[0].children.values,
                    listOf(),
                    mapOf(),
                    false,
                    listOf(),
                    listOf(),
                    listOf(),
                    false,
                    listOf(),
                    listOf(),
                    null
            )

            childTreeNodes.forEach { treeNode.add(it, x) }
        }
    }

    override fun compareTo(other: ModelNode<NodeHolder>) = when (other) {
        is NoteNode, is ImageNode -> 1
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
        is UnscheduledNode, is DividerNode -> -1
        else -> throw IllegalArgumentException()
    }

    fun addInstanceData(instanceData: GroupListFragment.InstanceData, x: TreeViewAdapter.Placeholder) {
        check(instanceData.instanceTimeStamp.toExactTimeStamp() == exactTimeStamp)

        check(instanceDatas.isNotEmpty())
        if (instanceDatas.size == 1) {
            check(notDoneInstanceNodes.isEmpty())

            treeNode.removeAll(x)
            singleInstanceNodeCollection = null

            val instanceData1 = instanceDatas.single()

            val notDoneInstanceNode = NotDoneInstanceNode(indentation, instanceData1, this@NotDoneGroupNode)
            notDoneInstanceNodes.add(notDoneInstanceNode)

            treeNode.add(notDoneInstanceNode.initialize(mapOf(), false, listOf(), listOf(), treeNode), x)
        }

        instanceDatas.add(instanceData)

        treeNode.add(newChildTreeNode(instanceData, mapOf(), false, listOf(), listOf()), x)
    }

    private fun newChildTreeNode(
            instanceData: GroupListFragment.InstanceData,
            expandedInstances: Map<InstanceKey, Boolean>,
            selected: Boolean,
            selectedInstances: List<InstanceKey>,
            selectedGroups: List<Long>
    ): TreeNode<NodeHolder> {
        val notDoneInstanceNode = NotDoneInstanceNode(indentation, instanceData, this)

        val childTreeNode = notDoneInstanceNode.initialize(
                expandedInstances,
                selected,
                selectedInstances,
                selectedGroups,
                treeNode
        )

        notDoneInstanceNodes.add(notDoneInstanceNode)

        return childTreeNode
    }

    fun expanded() = treeNode.isExpanded

    override val isSelectable = true

    override fun getOrdinal() = singleInstanceData.run { hierarchyData?.ordinal ?: ordinal }

    override fun setOrdinal(ordinal: Double) {
        singleInstanceData.let {
            if (it.hierarchyData != null) {
                it.hierarchyData.ordinal = ordinal

                DomainFactory.instance.setTaskHierarchyOrdinal(groupListFragment.parameters.dataId, it.hierarchyData)
            } else {
                it.ordinal = ordinal

                DomainFactory.instance.setInstanceOrdinal(groupListFragment.parameters.dataId, it.instanceKey, ordinal)
            }
        }
    }

    override val id: Any = if (singleInstance())
        SingleId(singleInstanceData.instanceKey)
    else
        GroupId(instanceDatas.map { it.instanceKey }.toSet(), exactTimeStamp)

    override val toggleDescendants get() = !singleInstance()

    data class SingleId(val instanceKey: InstanceKey)

    class GroupId(val instanceKeys: Set<InstanceKey>, val exactTimeStamp: ExactTimeStamp) {

        override fun hashCode() = 1

        override fun equals(other: Any?): Boolean {
            if (other === this)
                return true

            if (other !is GroupId)
                return false

            return instanceKeys == other.instanceKeys || exactTimeStamp == other.exactTimeStamp
        }
    }

    class NotDoneInstanceNode(
            indentation: Int,
            val instanceData: GroupListFragment.InstanceData,
            private val parentNotDoneGroupNode: NotDoneGroupNode
    ) : GroupHolderNode(indentation), NodeCollectionParent {

        companion object {

            fun getChildrenNew(
                    treeNode: TreeNode<NodeHolder>,
                    instanceData: GroupListFragment.InstanceData
            ) = instanceData.children
                    .values
                    .filter { it.done == null }
                    .let {
                        fun color() = if (!instanceData.taskCurrent) colorDisabled else colorSecondary

                        if (it.isNotEmpty() && !treeNode.isExpanded) {
                            val children = it.sorted().joinToString(", ") { it.name }

                            Pair(children, color())
                        } else if (!instanceData.note.isNullOrEmpty()) {
                            Pair(instanceData.note, color())
                        } else {
                            null
                        }
                    }
        }

        override val ripple = true

        override val isSelectable = true

        public override lateinit var treeNode: TreeNode<NodeHolder>
            private set

        private lateinit var nodeCollection: NodeCollection

        private val parentNotDoneGroupCollection get() = parentNotDoneGroupNode.notDoneGroupCollection

        val parentNodeCollection get() = parentNotDoneGroupCollection.nodeCollection

        private val groupListFragment get() = groupAdapter.groupListFragment

        fun initialize(
                expandedInstances: Map<InstanceKey, Boolean>,
                selected: Boolean,
                selectedInstances: List<InstanceKey>,
                selectedGroups: List<Long>,
                notDoneGroupTreeNode: TreeNode<NodeHolder>
        ): TreeNode<NodeHolder> {
            val (expanded, doneExpanded) = if (expandedInstances.containsKey(instanceData.instanceKey) && instanceData.children.isNotEmpty()) {
                true to expandedInstances.getValue(instanceData.instanceKey)
            } else {
                false to false
            }

            treeNode = TreeNode(this, notDoneGroupTreeNode, expanded, selected)

            nodeCollection = NodeCollection(indentation + 1, groupAdapter, false, treeNode, null)
            treeNode.setChildTreeNodes(nodeCollection.initialize(
                    instanceData.children.values,
                    listOf(),
                    expandedInstances,
                    doneExpanded,
                    selectedInstances,
                    selectedGroups,
                    listOf(),
                    false,
                    listOf(),
                    listOf(),
                    null
            ))

            return treeNode
        }

        fun addExpandedInstances(expandedInstances: MutableMap<InstanceKey, Boolean>) {
            if (!treeNode.isExpanded)
                return

            check(!expandedInstances.containsKey(instanceData.instanceKey))

            expandedInstances[instanceData.instanceKey] = nodeCollection.doneExpanded
            nodeCollection.addExpandedInstances(expandedInstances)
        }

        override val groupAdapter by lazy { parentNotDoneGroupNode.groupAdapter }

        override val name get() = NameData(instanceData.name, if (!instanceData.taskCurrent) colorDisabled else colorPrimary)

        override val children get() = getChildrenNew(treeNode, instanceData)

        override val checkBoxState
            get() = if (groupListFragment.selectionCallback.hasActionMode) {
                CheckBoxState.Invisible
            } else {
                CheckBoxState.Visible(false) {
                    val notDoneGroupTreeNode = parentNotDoneGroupNode.treeNode
                    check(notDoneGroupTreeNode.isExpanded)

                    val groupAdapter = parentNodeCollection.groupAdapter
                    val instanceKey = instanceData.instanceKey

                    groupAdapter.treeNodeCollection
                            .treeViewAdapter
                            .updateDisplayedNodes {
                                instanceData.done = DomainFactory.instance.setInstanceDone(groupAdapter.dataId, SaveService.Source.GUI, instanceKey, true)!!

                                GroupListFragment.recursiveExists(instanceData)

                                parentNotDoneGroupNode.remove(this, TreeViewAdapter.Placeholder)

                                parentNodeCollection.dividerNode.add(instanceData, TreeViewAdapter.Placeholder)
                            }

                    groupListFragment.listener.showSnackbarDone(1) {
                        DomainFactory.instance.setInstanceDone(0, SaveService.Source.GUI, instanceKey, false)
                    }
                }
            }

        override fun onClick(holder: NodeHolder) = groupListFragment.activity.startActivity(ShowInstanceActivity.getIntent(groupListFragment.activity, instanceData.instanceKey))

        override fun compareTo(other: ModelNode<NodeHolder>) = instanceData.compareTo((other as NotDoneInstanceNode).instanceData)

        fun removeFromParent(x: TreeViewAdapter.Placeholder) = parentNotDoneGroupNode.remove(this, x)

        override val id = Id(instanceData.instanceKey)

        override val deselectParent get() = true

        override val thumbnail = instanceData.imageState

        data class Id(val instanceKey: InstanceKey)
    }

    override fun ordinalDesc() = if (singleInstance()) {
        singleInstanceData.run { hierarchyData?.let { name + " " + it.ordinal } }
    } else {
        null
    }
}
