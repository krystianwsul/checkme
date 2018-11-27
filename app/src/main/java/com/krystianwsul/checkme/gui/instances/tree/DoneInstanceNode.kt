package com.krystianwsul.checkme.gui.instances.tree

import android.support.v7.widget.RecyclerView
import android.view.View
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.gui.instances.ShowInstanceActivity
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.utils.InstanceKey
import com.krystianwsul.treeadapter.ModelNode
import com.krystianwsul.treeadapter.ModelState
import com.krystianwsul.treeadapter.TreeNode
import com.krystianwsul.treeadapter.TreeViewAdapter

class DoneInstanceNode(indentation: Int, val instanceData: GroupListFragment.InstanceData, private val dividerNode: DividerNode) : GroupHolderNode(indentation), ModelNode, NodeCollectionParent {

    lateinit var treeNode: TreeNode
        private set

    private lateinit var nodeCollection: NodeCollection

    private val parentNodeCollection get() = dividerNode.nodeCollection

    private val groupListFragment get() = groupAdapter.mGroupListFragment

    fun initialize(dividerTreeNode: TreeNode, expandedInstances: Map<InstanceKey, Boolean>?): TreeNode {
        val expanded: Boolean
        val doneExpanded: Boolean
        if (expandedInstances != null && expandedInstances.containsKey(instanceData.InstanceKey) && !instanceData.children.isEmpty()) {
            expanded = true
            doneExpanded = expandedInstances[instanceData.InstanceKey]!!
        } else {
            expanded = false
            doneExpanded = false
        }

        treeNode = TreeNode(this, dividerTreeNode, expanded, false)

        nodeCollection = NodeCollection(indentation + 1, groupAdapter, false, this.treeNode, null)
        treeNode.setChildTreeNodes(nodeCollection.initialize(instanceData.children.values, null, expandedInstances, doneExpanded, null, false, null, false, null))

        return treeNode
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

    override val groupAdapter by lazy { parentNodeCollection.groupAdapter }

    override val name get() = Triple(instanceData.Name, if (!instanceData.TaskCurrent) colorDisabled else colorPrimary, true)

    override val details
        get() = instanceData.DisplayText
                .takeUnless { it.isNullOrEmpty() }
                ?.let { Pair(it, if (!instanceData.TaskCurrent) colorDisabled else colorSecondary) }

    override val children get() = NotDoneGroupNode.NotDoneInstanceNode.getChildrenNew(treeNode, instanceData)

    override val expand
        get() = if (instanceData.children.isEmpty()) {
            null
        } else {
            Pair(if (this.treeNode.isExpanded) R.drawable.ic_expand_less_black_36dp else R.drawable.ic_expand_more_black_36dp, treeNode.expandListener)
        }

    override val checkBoxVisibility = View.VISIBLE

    override val checkBoxChecked = true

    override val checkBoxOnClickListener: () -> Unit
        get() {
            val nodeCollection = dividerNode.nodeCollection

            val groupAdapter = nodeCollection.groupAdapter

            return {
                groupAdapter.treeNodeCollection
                        .treeViewAdapter
                        .updateDisplayedNodes {
                            instanceData.Done = DomainFactory.getKotlinDomainFactory().setInstanceDone(groupAdapter.mDataId, SaveService.Source.GUI, instanceData.InstanceKey, false)

                            dividerNode.remove(this, TreeViewAdapter.Placeholder)

                            nodeCollection.notDoneGroupCollection.add(instanceData, TreeViewAdapter.Placeholder)
                        }

                groupAdapter.mGroupListFragment.updateSelectAll()
            }
        }

    override val separatorVisible get() = treeNode.separatorVisible

    override fun getOnLongClickListener(viewHolder: RecyclerView.ViewHolder) = treeNode.onLongClickListener

    override val onClickListener get() = treeNode.onClickListener

    override fun compareTo(other: ModelNode): Int {
        checkNotNull(instanceData.Done)

        val doneInstanceNode = other as DoneInstanceNode
        checkNotNull(doneInstanceNode.instanceData.Done)

        return -instanceData.Done!!.compareTo(doneInstanceNode.instanceData.Done!!) // negate
    }

    override val isSelectable = false

    override fun onClick() = groupListFragment.activity.startActivity(ShowInstanceActivity.getIntent(groupListFragment.activity, instanceData.InstanceKey))

    override val isVisibleWhenEmpty = true

    override val isVisibleDuringActionMode = true

    override val isSeparatorVisibleWhenNotExpanded = false

    fun removeFromParent(x: TreeViewAdapter.Placeholder) = dividerNode.remove(this, x)

    override val id = instanceData.InstanceKey

    override val state get() = State(instanceData.copy())

    data class State(val instanceData: GroupListFragment.InstanceData) : ModelState {

        override fun same(other: ModelState) = (other as? State)?.instanceData?.InstanceKey == instanceData.InstanceKey
    }
}
