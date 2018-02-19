package com.krystianwsul.checkme.gui.instances.tree

import android.graphics.Color
import android.support.v4.content.ContextCompat
import android.view.View
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.utils.InstanceKey
import com.krystianwsul.treeadapter.ModelNode
import com.krystianwsul.treeadapter.NodeContainer
import com.krystianwsul.treeadapter.TreeNode
import junit.framework.Assert
import java.util.*

class DividerNode(density: Float, indentation: Int, val nodeCollection: NodeCollection) : GroupHolderNode(density, indentation), ModelNode {

    private lateinit var treeNode: TreeNode

    private val doneInstanceNodes = ArrayList<DoneInstanceNode>()

    val groupAdapter get() = nodeCollection.groupAdapter

    private val groupListFragment get() = groupAdapter.mGroupListFragment

    fun initialize(expanded: Boolean, nodeContainer: NodeContainer, doneInstanceDatas: List<GroupListFragment.InstanceData>, expandedInstances: Map<InstanceKey, Boolean>?): TreeNode {
        Assert.assertTrue(!expanded || !doneInstanceDatas.isEmpty())

        treeNode = TreeNode(this, nodeContainer, expanded, false)

        val childTreeNodes = doneInstanceDatas.map { newChildTreeNode(it, expandedInstances) }

        treeNode.setChildTreeNodes(childTreeNodes)

        return treeNode
    }

    private fun newChildTreeNode(instanceData: GroupListFragment.InstanceData, expandedInstances: Map<InstanceKey, Boolean>?): TreeNode {
        Assert.assertTrue(instanceData.Done != null)

        val doneInstanceNode = DoneInstanceNode(mDensity, mIndentation, instanceData, this)

        val childTreeNode = doneInstanceNode.initialize(treeNode, expandedInstances)

        doneInstanceNodes.add(doneInstanceNode)

        return childTreeNode
    }

    fun expanded() = treeNode.expanded()

    fun addExpandedInstances(expandedInstances: Map<InstanceKey, Boolean>) {
        for (doneInstanceNode in doneInstanceNodes)
            doneInstanceNode.addExpandedInstances(expandedInstances)
    }

    override fun getNameVisibility() = View.VISIBLE

    override fun getName() = groupListFragment.getString(R.string.done)

    override fun getNameColor() = ContextCompat.getColor(groupListFragment.activity!!, R.color.textPrimary)

    override fun getNameSingleLine() = true

    override fun getDetailsVisibility() = View.GONE

    override fun getDetails() = throw UnsupportedOperationException()

    override fun getDetailsColor() = throw UnsupportedOperationException()

    override fun getChildren(): Nothing? = null

    override fun getExpandVisibility(): Int {
        Assert.assertTrue(this.treeNode.expandVisible)

        return View.VISIBLE
    }

    override fun getExpandImageResource(): Int {
        Assert.assertTrue(this.treeNode.expandVisible)

        return if (this.treeNode.expanded())
            R.drawable.ic_expand_less_black_36dp
        else
            R.drawable.ic_expand_more_black_36dp
    }

    override fun getExpandOnClickListener(): View.OnClickListener {
        Assert.assertTrue(this.treeNode.expandVisible)

        return this.treeNode.expandListener
    }

    override fun getCheckBoxVisibility() = View.INVISIBLE

    override fun getCheckBoxChecked() = throw UnsupportedOperationException()

    override fun getCheckBoxOnClickListener() = throw UnsupportedOperationException()

    override fun getSeparatorVisibility() = if (treeNode.separatorVisibility) View.VISIBLE else View.INVISIBLE

    override fun getBackgroundColor() = Color.TRANSPARENT

    override fun getOnLongClickListener() = treeNode.onLongClickListener

    override fun getOnClickListener() = treeNode.onClickListener

    fun remove(doneInstanceNode: DoneInstanceNode) {
        Assert.assertTrue(doneInstanceNodes.contains(doneInstanceNode))
        doneInstanceNodes.remove(doneInstanceNode)

        treeNode.remove(doneInstanceNode.treeNode)
    }

    fun add(instanceData: GroupListFragment.InstanceData) {
        treeNode.add(newChildTreeNode(instanceData, null))
    }

    override fun selectable() = false

    override fun onClick() = Unit

    override fun compareTo(other: ModelNode): Int {
        Assert.assertTrue(other is NoteNode || other is NotDoneGroupNode || other is UnscheduledNode)
        return 1
    }

    override fun visibleWhenEmpty() = false

    override fun visibleDuringActionMode() = false

    override fun separatorVisibleWhenNotExpanded() = false
}
