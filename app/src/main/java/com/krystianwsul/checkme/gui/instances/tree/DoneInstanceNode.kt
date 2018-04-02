package com.krystianwsul.checkme.gui.instances.tree

import android.graphics.Color
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.view.View
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.gui.instances.ShowInstanceActivity
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.utils.InstanceKey
import com.krystianwsul.treeadapter.ModelNode
import com.krystianwsul.treeadapter.TreeNode

class DoneInstanceNode(density: Float, indentation: Int, val instanceData: GroupListFragment.InstanceData, private val dividerNode: DividerNode) : GroupHolderNode(density, indentation), ModelNode, NodeCollectionParent {

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

        nodeCollection = NodeCollection(mDensity, mIndentation + 1, groupAdapter, false, this.treeNode, null)
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

    override fun getGroupAdapter() = parentNodeCollection.groupAdapter

    override fun getName() = Triple(instanceData.Name, ContextCompat.getColor(groupListFragment.activity!!, if (!instanceData.TaskCurrent) R.color.textDisabled else R.color.textPrimary), true)

    override fun getDetails() = instanceData.DisplayText
            .takeUnless { it.isNullOrEmpty() }
            ?.let { Pair(it, ContextCompat.getColor(dividerNode.groupAdapter.mGroupListFragment.activity!!, if (!instanceData.TaskCurrent) R.color.textDisabled else R.color.textSecondary)) }

    override fun getChildren() = NotDoneGroupNode.NotDoneInstanceNode.getChildrenNew(treeNode, instanceData, groupListFragment)

    override fun getExpandVisibility(): Int {
        return if (instanceData.children.isEmpty()) {
            check(!this.treeNode.expandVisible)

            View.INVISIBLE
        } else {
            check(this.treeNode.expandVisible)

            View.VISIBLE
        }
    }

    override fun getExpandImageResource(): Int {
        check(!instanceData.children.isEmpty())
        check(this.treeNode.expandVisible)

        return if (this.treeNode.isExpanded)
            R.drawable.ic_expand_less_black_36dp
        else
            R.drawable.ic_expand_more_black_36dp
    }

    override fun getExpandOnClickListener(): View.OnClickListener {
        check(!instanceData.children.isEmpty())
        check(treeNode.expandVisible)

        return treeNode.expandListener
    }

    override fun getCheckBoxVisibility() = View.VISIBLE

    override fun getCheckBoxChecked() = true

    override fun getCheckBoxOnClickListener(): View.OnClickListener {
        val nodeCollection = dividerNode.nodeCollection

        val groupAdapter = nodeCollection.groupAdapter

        return View.OnClickListener { v ->
            v.setOnClickListener(null)

            instanceData.Done = DomainFactory.getDomainFactory().setInstanceDone(groupAdapter.mGroupListFragment.activity!!, groupAdapter.mDataId, SaveService.Source.GUI, instanceData.InstanceKey, false)
            check(instanceData.Done == null)

            dividerNode.remove(this)

            nodeCollection.notDoneGroupCollection.add(instanceData)

            groupAdapter.mGroupListFragment.updateSelectAll()
        }
    }

    override fun getSeparatorVisibility() = if (treeNode.separatorVisibility) View.VISIBLE else View.INVISIBLE

    override fun getBackgroundColor() = Color.TRANSPARENT

    override fun getOnLongClickListener(viewHolder: RecyclerView.ViewHolder) = treeNode.onLongClickListener

    override fun getOnClickListener() = treeNode.onClickListener

    override fun compareTo(other: ModelNode): Int {
        checkNotNull(instanceData.Done)

        val doneInstanceNode = other as DoneInstanceNode
        checkNotNull(doneInstanceNode.instanceData.Done)

        return -instanceData.Done!!.compareTo(doneInstanceNode.instanceData.Done!!) // negate
    }

    override val isSelectable = false

    override fun onClick() = groupListFragment.activity!!.startActivity(ShowInstanceActivity.getIntent(groupListFragment.activity!!, instanceData.InstanceKey))

    override val isVisibleWhenEmpty = true

    override val isVisibleDuringActionMode = true

    override val isSeparatorVisibleWhenNotExpanded = false

    fun removeFromParent() {
        dividerNode.remove(this)
    }
}
