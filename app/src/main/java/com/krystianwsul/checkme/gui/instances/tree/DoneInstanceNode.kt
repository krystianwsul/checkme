package com.krystianwsul.checkme.gui.instances.tree

import android.graphics.Color
import android.support.v4.content.ContextCompat
import android.view.View
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.gui.instances.ShowInstanceActivity
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.utils.InstanceKey
import com.krystianwsul.treeadapter.ModelNode
import com.krystianwsul.treeadapter.TreeNode
import junit.framework.Assert

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

    private fun expanded() = treeNode.expanded()

    fun addExpandedInstances(expandedInstances: Map<InstanceKey, Boolean>) {
        if (!expanded())
            return

        Assert.assertTrue(!expandedInstances.containsKey(instanceData.InstanceKey))

        nodeCollection.addExpandedInstances(expandedInstances.toMutableMap().also {
            it[instanceData.InstanceKey] = nodeCollection.doneExpanded
        })
    }

    override fun getGroupAdapter() = parentNodeCollection.groupAdapter

    override fun getNameVisibility() = View.VISIBLE

    override fun getName() = instanceData.Name

    override fun getNameColor(): Int {
        val groupListFragment = groupListFragment

        return ContextCompat.getColor(groupListFragment.activity!!, if (!instanceData.TaskCurrent) {
            R.color.textDisabled
        } else {
            R.color.textPrimary
        })
    }

    override fun getNameSingleLine() = true

    override fun getDetailsVisibility(): Int {
        return if (instanceData.DisplayText.isNullOrEmpty()) {
            View.GONE
        } else {
            View.VISIBLE
        }
    }

    override fun getDetails(): String {
        Assert.assertTrue(!instanceData.DisplayText.isNullOrEmpty())
        return instanceData.DisplayText!!
    }

    override fun getDetailsColor(): Int {
        return ContextCompat.getColor(dividerNode.groupAdapter.mGroupListFragment.activity!!, if (!instanceData.TaskCurrent) {
            R.color.textDisabled
        } else {
            R.color.textSecondary
        })
    }

    override fun getChildrenVisibility(): Int {
        return if ((instanceData.children.isEmpty() || expanded()) && instanceData.mNote.isNullOrEmpty()) {
            View.GONE
        } else {
            View.VISIBLE
        }
    }

    override fun getChildren(): String {
        Assert.assertTrue(!instanceData.children.isEmpty() && !expanded() || !instanceData.mNote.isNullOrEmpty())

        val notDoneInstanceDatas = instanceData.children
                .values
                .filter { it.Done == null }

        return if (notDoneInstanceDatas.isNotEmpty() && !expanded()) {
            notDoneInstanceDatas.sortedBy { it.mTaskStartExactTimeStamp }.joinToString(", ") { it.Name }
        } else {
            Assert.assertTrue(!instanceData.mNote.isNullOrEmpty())

            instanceData.mNote!!
        }
    }

    override fun getChildrenColor(): Int {
        Assert.assertTrue(!instanceData.children.isEmpty() && !expanded() || !instanceData.mNote.isNullOrEmpty())

        val activity = groupListFragment.activity!!

        return ContextCompat.getColor(activity, if (!instanceData.TaskCurrent) {
            R.color.textDisabled
        } else {
            R.color.textSecondary
        })
    }

    override fun getExpandVisibility(): Int {
        return if (instanceData.children.isEmpty()) {
            Assert.assertTrue(!this.treeNode.expandVisible)

            View.INVISIBLE
        } else {
            Assert.assertTrue(this.treeNode.expandVisible)

            View.VISIBLE
        }
    }

    override fun getExpandImageResource(): Int {
        Assert.assertTrue(!instanceData.children.isEmpty())
        Assert.assertTrue(this.treeNode.expandVisible)

        return if (this.treeNode.expanded())
            R.drawable.ic_expand_less_black_36dp
        else
            R.drawable.ic_expand_more_black_36dp
    }

    override fun getExpandOnClickListener(): View.OnClickListener {
        Assert.assertTrue(!instanceData.children.isEmpty())
        Assert.assertTrue(treeNode.expandVisible)

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
            Assert.assertTrue(instanceData.Done == null)

            dividerNode.remove(this)

            nodeCollection.notDoneGroupCollection.add(instanceData)

            groupAdapter.mGroupListFragment.updateSelectAll()
        }
    }

    override fun getSeparatorVisibility() = if (this.treeNode.separatorVisibility) View.VISIBLE else View.INVISIBLE

    override fun getBackgroundColor() = Color.TRANSPARENT

    override fun getOnLongClickListener() = treeNode.onLongClickListener

    override fun getOnClickListener() = this.treeNode.onClickListener

    override fun compareTo(other: ModelNode): Int {
        Assert.assertTrue(instanceData.Done != null)

        val doneInstanceNode = other as DoneInstanceNode
        Assert.assertTrue(doneInstanceNode.instanceData.Done != null)

        return -instanceData.Done!!.compareTo(doneInstanceNode.instanceData.Done!!) // negate
    }

    override fun selectable() = false

    override fun onClick() = groupListFragment.activity!!.startActivity(ShowInstanceActivity.getIntent(groupListFragment.activity!!, instanceData.InstanceKey))

    override fun visibleWhenEmpty() = true

    override fun visibleDuringActionMode() = true

    override fun separatorVisibleWhenNotExpanded() = false

    fun removeFromParent() {
        dividerNode.remove(this)
    }
}
