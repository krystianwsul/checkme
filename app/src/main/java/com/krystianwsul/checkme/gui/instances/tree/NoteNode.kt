package com.krystianwsul.checkme.gui.instances.tree

import android.graphics.Color
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.view.View
import com.krystianwsul.checkme.R
import com.krystianwsul.treeadapter.ModelNode
import com.krystianwsul.treeadapter.NodeContainer
import com.krystianwsul.treeadapter.TreeNode

import java.util.*

class NoteNode(density: Float, private val note: String, private val groupListFragment: GroupListFragment) : GroupHolderNode(density, 0), ModelNode {

    private lateinit var treeNode: TreeNode

    init {
        check(note.isNotEmpty())
    }

    fun initialize(nodeContainer: NodeContainer): TreeNode {
        treeNode = TreeNode(this, nodeContainer, false, false)

        treeNode.setChildTreeNodes(ArrayList())
        return treeNode
    }

    override val name get() = Triple(note, ContextCompat.getColor(groupListFragment.activity, R.color.textPrimary), false)

    override val checkBoxVisibility = View.GONE

    override val checkBoxChecked get() = throw UnsupportedOperationException()

    override val checkBoxOnClickListener get() = throw UnsupportedOperationException()

    override val separatorVisibility get() = if (this.treeNode.separatorVisibility) View.VISIBLE else View.INVISIBLE

    override val backgroundColor = Color.TRANSPARENT

    override fun getOnLongClickListener(viewHolder: RecyclerView.ViewHolder): View.OnLongClickListener? = null

    override val onClickListener: View.OnClickListener? = null

    override val isSelectable = false

    override fun onClick() = Unit

    override val isVisibleWhenEmpty = true

    override val isVisibleDuringActionMode = false

    override val isSeparatorVisibleWhenNotExpanded = true

    override fun compareTo(other: ModelNode): Int {
        check(other is NotDoneGroupNode || other is UnscheduledNode || other is DividerNode)

        return -1
    }

    override fun hashCode() = note.hashCode()

    override fun equals(other: Any?) = (other as? NoteNode)?.note == note

    override val id = 98765
}
