package com.krystianwsul.checkme.gui.instances.tree

import android.graphics.Color
import android.support.v4.content.ContextCompat
import android.view.View
import com.krystianwsul.checkme.R
import com.krystianwsul.treeadapter.ModelNode
import com.krystianwsul.treeadapter.NodeContainer
import com.krystianwsul.treeadapter.TreeNode
import junit.framework.Assert
import java.util.*

class NoteNode(density: Float, private val note: String, private val groupListFragment: GroupListFragment) : GroupHolderNode(density, 0), ModelNode {

    private lateinit var treeNode: TreeNode

    init {
        Assert.assertTrue(note.isNotEmpty())
    }

    fun initialize(nodeContainer: NodeContainer): TreeNode {
        treeNode = TreeNode(this, nodeContainer, false, false)

        treeNode.setChildTreeNodes(ArrayList())
        return treeNode
    }

    override fun getNameVisibility() = View.VISIBLE

    override fun getName() = note

    override fun getNameColor() = ContextCompat.getColor(groupListFragment.activity!!, R.color.textPrimary)

    override fun getNameSingleLine() = false

    override fun getDetailsVisibility() = View.GONE

    override fun getDetails() = throw UnsupportedOperationException()

    override fun getDetailsColor() = throw UnsupportedOperationException()

    override fun getChildrenVisibility() = View.GONE

    override fun getChildren() = throw UnsupportedOperationException()

    override fun getChildrenColor() = throw UnsupportedOperationException()

    override fun getExpandVisibility() = View.GONE

    override fun getExpandImageResource() = throw UnsupportedOperationException()

    override fun getExpandOnClickListener() = throw UnsupportedOperationException()

    override fun getCheckBoxVisibility() = View.GONE

    override fun getCheckBoxChecked() = throw UnsupportedOperationException()

    override fun getCheckBoxOnClickListener() = throw UnsupportedOperationException()

    override fun getSeparatorVisibility() = if (this.treeNode.separatorVisibility) View.VISIBLE else View.INVISIBLE

    override fun getBackgroundColor() = Color.TRANSPARENT

    override fun getOnLongClickListener(): View.OnLongClickListener? = null

    override fun getOnClickListener(): View.OnClickListener? = null

    override fun selectable() = false

    override fun onClick() = Unit

    override fun visibleWhenEmpty() = true

    override fun visibleDuringActionMode() = false

    override fun separatorVisibleWhenNotExpanded() = true

    override fun compareTo(other: ModelNode): Int {
        Assert.assertTrue(other is NotDoneGroupNode || other is UnscheduledNode || other is DividerNode)

        return -1
    }
}
