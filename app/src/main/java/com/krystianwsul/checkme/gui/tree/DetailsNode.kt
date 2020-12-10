package com.krystianwsul.checkme.gui.tree

import androidx.recyclerview.widget.RecyclerView
import com.krystianwsul.checkme.databinding.RowListDetailsBinding
import com.krystianwsul.checkme.gui.tree.delegates.invisible_checkbox.InvisibleCheckboxDelegate
import com.krystianwsul.checkme.gui.tree.delegates.invisible_checkbox.InvisibleCheckboxHolder
import com.krystianwsul.checkme.gui.tree.delegates.invisible_checkbox.InvisibleCheckboxModelNode
import com.krystianwsul.checkme.gui.utils.SearchData
import com.krystianwsul.common.utils.normalized
import com.krystianwsul.treeadapter.ModelNode
import com.krystianwsul.treeadapter.ModelState
import com.krystianwsul.treeadapter.NodeContainer
import com.krystianwsul.treeadapter.TreeNode

class DetailsNode(
        private val details: String,
        instance: Boolean,
        override val parentNode: ModelNode<AbstractHolder>?,
) : AbstractModelNode(), InvisibleCheckboxModelNode {

    override lateinit var treeNode: TreeNode<AbstractHolder>
        private set

    private lateinit var nodeContainer: NodeContainer<AbstractHolder>

    override val holderType = HolderType.DETAILS

    override val id get() = Id(nodeContainer.id)

    override val state get() = State(super.state, details)

    data class Id(val id: Any)

    private val normalizedNote by lazy { details.normalized() }

    override val disableRipple = true

    init {
        check(details.isNotEmpty())
    }

    fun initialize(nodeContainer: NodeContainer<AbstractHolder>): TreeNode<AbstractHolder> {
        this.nodeContainer = nodeContainer

        treeNode = TreeNode(this, nodeContainer, expanded = false, selected = false)
        treeNode.setChildTreeNodes(listOf())

        return treeNode
    }

    override val isVisibleDuringActionMode = false

    override val isSeparatorVisibleWhenNotExpanded = true

    override val delegates by lazy {
        listOf(
                InvisibleCheckboxDelegate(this)
        )
    }

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, startingDrag: Boolean) {
        super.onBindViewHolder(viewHolder, startingDrag)

        (viewHolder as Holder).rowText.text = details
    }

    override fun compareTo(other: ModelNode<AbstractHolder>) = -1

    override val checkBoxInvisible = instance

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
    ) : AbstractHolder(binding.root), InvisibleCheckboxHolder {

        val rowText = binding.rowListDetailsText
        override val rowCheckBoxFrame = binding.rowListDetailsCheckboxInclude.rowCheckboxFrame
        override val rowMarginStart = binding.rowListDetailsMargin
        override val rowSeparator = binding.rowListDetailsSeparator
    }
}
