package com.krystianwsul.checkme.gui.instances.tree

import android.graphics.Color
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
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

    override fun getName() = Triple(note, ContextCompat.getColor(groupListFragment.activity!!, R.color.textPrimary), false)

    override fun getExpandVisibility() = View.GONE

    override fun getExpandImageResource() = throw UnsupportedOperationException()

    override fun getExpandOnClickListener() = throw UnsupportedOperationException()

    override fun getCheckBoxVisibility() = View.GONE

    override fun getCheckBoxChecked() = throw UnsupportedOperationException()

    override fun getCheckBoxOnClickListener() = throw UnsupportedOperationException()

    override fun getSeparatorVisibility() = if (this.treeNode.separatorVisibility) View.VISIBLE else View.INVISIBLE

    override fun getBackgroundColor() = Color.TRANSPARENT

    override fun getOnLongClickListener(viewHolder: RecyclerView.ViewHolder): View.OnLongClickListener? = null

    override fun getOnClickListener(): View.OnClickListener? = null

    override val isSelectable = false

    override fun onClick() = Unit

    override val isVisibleWhenEmpty = true

    override val isVisibleDuringActionMode = false

    override val isSeparatorVisibleWhenNotExpanded = true

    override fun compareTo(other: ModelNode): Int {
        Assert.assertTrue(other is NotDoneGroupNode || other is UnscheduledNode || other is DividerNode)

        return -1
    }
}
