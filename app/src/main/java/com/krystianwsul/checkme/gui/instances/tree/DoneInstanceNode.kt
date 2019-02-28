package com.krystianwsul.checkme.gui.instances.tree

import android.view.View
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.gui.instances.ShowInstanceActivity
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.utils.InstanceKey
import com.krystianwsul.treeadapter.ModelNode
import com.krystianwsul.treeadapter.TreeNode
import com.krystianwsul.treeadapter.TreeViewAdapter

class DoneInstanceNode(
        indentation: Int,
        val instanceData: GroupListFragment.InstanceData,
        val dividerNode: DividerNode) : GroupHolderNode(indentation), NodeCollectionParent {

    public override lateinit var treeNode: TreeNode
        private set

    override val ripple = true

    private lateinit var nodeCollection: NodeCollection

    private val parentNodeCollection get() = dividerNode.nodeCollection

    private val groupListFragment get() = groupAdapter.groupListFragment

    fun initialize(dividerTreeNode: TreeNode, expandedInstances: Map<InstanceKey, Boolean>, selectedInstances: List<InstanceKey>): TreeNode {
        val selected = selectedInstances.contains(instanceData.instanceKey)

        val expanded: Boolean
        val doneExpanded: Boolean
        if (expandedInstances.containsKey(instanceData.instanceKey) && !instanceData.children.isEmpty()) {
            expanded = true
            doneExpanded = expandedInstances[instanceData.instanceKey]!!
        } else {
            expanded = false
            doneExpanded = false
        }

        treeNode = TreeNode(this, dividerTreeNode, expanded, selected)

        nodeCollection = NodeCollection(indentation + 1, groupAdapter, false, this.treeNode, null)
        treeNode.setChildTreeNodes(nodeCollection.initialize(instanceData.children.values, listOf(), expandedInstances, doneExpanded, listOf(), listOf(), listOf(), false, listOf(), listOf()))

        return treeNode
    }

    private fun expanded() = treeNode.isExpanded

    fun addExpandedInstances(expandedInstances: MutableMap<InstanceKey, Boolean>) {
        if (!expanded())
            return

        check(!expandedInstances.containsKey(instanceData.instanceKey))

        expandedInstances[instanceData.instanceKey] = nodeCollection.doneExpanded
        nodeCollection.addExpandedInstances(expandedInstances)
    }

    override val groupAdapter by lazy { parentNodeCollection.groupAdapter }

    override val name get() = Triple(instanceData.name, if (!instanceData.taskCurrent) colorDisabled else colorPrimary, true)

    override val details
        get() = instanceData.displayText
                .takeUnless { it.isNullOrEmpty() }
                ?.let { Pair(it, if (!instanceData.taskCurrent) colorDisabled else colorSecondary) }

    override val children get() = NotDoneGroupNode.NotDoneInstanceNode.getChildrenNew(treeNode, instanceData)

    override val checkBoxVisibility
        get() = if (groupListFragment.selectionCallback.hasActionMode) {
            View.INVISIBLE
        } else {
            View.VISIBLE
        }

    override val checkBoxChecked = true

    override fun checkBoxOnClickListener() {
        val nodeCollection = dividerNode.nodeCollection

        val groupAdapter = nodeCollection.groupAdapter

        groupAdapter.treeNodeCollection
                .treeViewAdapter
                .updateDisplayedNodes {
                    instanceData.done = DomainFactory.instance.setInstanceDone(groupAdapter.dataId, SaveService.Source.GUI, instanceData.instanceKey, false)

                    dividerNode.remove(this, TreeViewAdapter.Placeholder)

                    nodeCollection.notDoneGroupCollection.add(instanceData, TreeViewAdapter.Placeholder)
                }
    }

    override fun compareTo(other: ModelNode): Int {
        checkNotNull(instanceData.done)

        val doneInstanceNode = other as DoneInstanceNode
        checkNotNull(doneInstanceNode.instanceData.done)

        return -instanceData.done!!.compareTo(doneInstanceNode.instanceData.done!!) // negate
    }

    override val isSelectable = true

    override fun onClick() = groupListFragment.activity.startActivity(ShowInstanceActivity.getIntent(groupListFragment.activity, instanceData.instanceKey))

    fun removeFromParent(x: TreeViewAdapter.Placeholder) {
        dividerNode.remove(this, x)

        treeNode.deselect(x)
    }

    override val id = instanceData.instanceKey
}
