package com.krystianwsul.checkme.gui.instances.tree

import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.view.View
import com.krystianwsul.checkme.R
import com.krystianwsul.treeadapter.ModelNode
import com.krystianwsul.treeadapter.ModelState
import com.krystianwsul.treeadapter.NodeContainer
import com.krystianwsul.treeadapter.TreeNode
import java.util.*

class NoteNode(private val note: String, private val groupListFragment: GroupListFragment) : GroupHolderNode(0), ModelNode {

    private lateinit var treeNode: TreeNode
    private lateinit var nodeContainer: NodeContainer

    init {
        check(note.isNotEmpty())
    }

    fun initialize(nodeContainer: NodeContainer): TreeNode {
        this.nodeContainer = nodeContainer
        treeNode = TreeNode(this, nodeContainer, false, false)

        treeNode.setChildTreeNodes(ArrayList())
        return treeNode
    }

    override val name get() = Triple(note, ContextCompat.getColor(groupListFragment.activity, R.color.textPrimary), false)

    override val checkBoxVisibility = View.GONE

    override val separatorVisibility get() = if (treeNode.separatorVisibility) View.VISIBLE else View.INVISIBLE

    override fun getOnLongClickListener(viewHolder: RecyclerView.ViewHolder) = treeNode.onLongClickListener

    override val onClickListener get() = treeNode.onClickListener

    override val isSelectable = false

    override fun onClick() = Unit

    override val isVisibleWhenEmpty = true

    override val isVisibleDuringActionMode = false

    override val isSeparatorVisibleWhenNotExpanded = true

    override fun compareTo(other: ModelNode): Int {
        check(other is NotDoneGroupNode || other is UnscheduledNode || other is DividerNode)

        return -1
    }

    override val state get() = State(nodeContainer.id, note)

    data class State(val id: Any, val note: String) : ModelState {

        override fun same(other: ModelState) = (other as? State)?.id == id
    }
}
