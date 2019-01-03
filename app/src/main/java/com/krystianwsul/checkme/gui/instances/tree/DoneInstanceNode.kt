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
        val selected = selectedInstances.contains(instanceData.InstanceKey)

        val expanded: Boolean
        val doneExpanded: Boolean
        if (expandedInstances.containsKey(instanceData.InstanceKey) && !instanceData.children.isEmpty()) {
            expanded = true
            doneExpanded = expandedInstances[instanceData.InstanceKey]!!
        } else {
            expanded = false
            doneExpanded = false
        }

        treeNode = TreeNode(this, dividerTreeNode, expanded, selected)

        nodeCollection = NodeCollection(indentation + 1, groupAdapter, false, this.treeNode, null)
        treeNode.setChildTreeNodes(nodeCollection.initialize(instanceData.children.values, listOf(), expandedInstances, doneExpanded, listOf(), listOf(), listOf(), false, listOf()))

        return treeNode
    }

    private fun expanded() = treeNode.isExpanded

    fun addExpandedInstances(expandedInstances: MutableMap<InstanceKey, Boolean>) {
        if (!expanded())
            return

        check(!expandedInstances.containsKey(instanceData.InstanceKey))

        expandedInstances[instanceData.InstanceKey] = nodeCollection.doneExpanded
        nodeCollection.addExpandedInstances(expandedInstances)
    }

    override val groupAdapter by lazy { parentNodeCollection.groupAdapter }

    override val name get() = Triple(instanceData.name, if (!instanceData.TaskCurrent) colorDisabled else colorPrimary, true)

    override val details
        get() = instanceData.DisplayText
                .takeUnless { it.isNullOrEmpty() }
                ?.let { Pair(it, if (!instanceData.TaskCurrent) colorDisabled else colorSecondary) }

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
                    instanceData.Done = DomainFactory.getInstance().setInstanceDone(groupAdapter.dataId, SaveService.Source.GUI, instanceData.InstanceKey, false)

                    dividerNode.remove(this, TreeViewAdapter.Placeholder)

                    nodeCollection.notDoneGroupCollection.add(instanceData, TreeViewAdapter.Placeholder)
                }

        groupAdapter.groupListFragment.updateSelectAll()
    }

    override fun compareTo(other: ModelNode): Int {
        checkNotNull(instanceData.Done)

        val doneInstanceNode = other as DoneInstanceNode
        checkNotNull(doneInstanceNode.instanceData.Done)

        return -instanceData.Done!!.compareTo(doneInstanceNode.instanceData.Done!!) // negate
    }

    override val isSelectable = true

    override fun onClick() = groupListFragment.activity.startActivity(ShowInstanceActivity.getIntent(groupListFragment.activity, instanceData.InstanceKey))

    fun removeFromParent(x: TreeViewAdapter.Placeholder) = dividerNode.remove(this, x)

    override val id = instanceData.InstanceKey
}
