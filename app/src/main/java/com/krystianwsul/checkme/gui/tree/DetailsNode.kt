package com.krystianwsul.checkme.gui.tree

import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.krystianwsul.checkme.databinding.RowListDetailsBinding
import com.krystianwsul.checkme.gui.tree.delegates.indentation.IndentationDelegate
import com.krystianwsul.checkme.gui.tree.delegates.indentation.IndentationHolder
import com.krystianwsul.checkme.gui.tree.delegates.indentation.IndentationModelNode
import com.krystianwsul.checkme.gui.utils.SearchData
import com.krystianwsul.common.utils.normalized
import com.krystianwsul.treeadapter.ModelNode
import com.krystianwsul.treeadapter.ModelState
import com.krystianwsul.treeadapter.NodeContainer
import com.krystianwsul.treeadapter.TreeNode

class DetailsNode(
        private val assignedTo: String,
        private val note: String?,
        override val parentNode: ModelNode<AbstractHolder>?,
        indentation: Int,
) : AbstractModelNode(), IndentationModelNode {

    override lateinit var treeNode: TreeNode<AbstractHolder>
        private set

    private lateinit var nodeContainer: NodeContainer<AbstractHolder>

    override val indentation = indentation - 1

    override val holderType = HolderType.DETAILS

    override val id get() = Id(nodeContainer.id)

    override val state get() = State(super.state, assignedTo)

    data class Id(val id: Any)

    private val normalizedNote by lazy { assignedTo.normalized() }

    override val disableRipple = true

    fun initialize(nodeContainer: NodeContainer<AbstractHolder>): TreeNode<AbstractHolder> {
        this.nodeContainer = nodeContainer

        treeNode = TreeNode(this, nodeContainer, expanded = false, selected = false)
        treeNode.setChildTreeNodes(listOf())

        return treeNode
    }

    override val isVisibleDuringActionMode = false

    override val delegates by lazy { listOf(IndentationDelegate(this)) }

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, startingDrag: Boolean) {
        super.onBindViewHolder(viewHolder, startingDrag)

        (viewHolder as Holder).apply {
            rowAssignedTo.apply {
                isVisible = assignedTo.isNotEmpty()
                text = "assigned to: $assignedTo"
            }

            rowNote.apply {
                isVisible = !note.isNullOrEmpty()
                text = note
            }
        }
    }

    override fun compareTo(other: ModelNode<AbstractHolder>) = -1

    override fun normalize() {
        normalizedNote
    }

    override fun matches(filterCriteria: Any?): Boolean {
        if (filterCriteria == null) return true

        val query = (filterCriteria as SearchData).query

        if (query.isEmpty()) return true

        return normalizedNote.contains(query)
    }

    override fun canBeShownWithFilterCriteria(filterCriteria: Any?) = false

    data class State(val superState: ModelState, val note: String) : ModelState {

        override val id = superState.id
    }

    class Holder(
            override val baseAdapter: BaseAdapter,
            binding: RowListDetailsBinding,
    ) : AbstractHolder(binding.root), IndentationHolder {

        override val rowContainer = binding.rowListDetailsContainer
        val rowAssignedTo = binding.rowListDetailsAssignedTo
        val rowNote = binding.rowListDetailsNote
        override val rowSeparator = binding.rowListDetailsSeparator
    }
}
