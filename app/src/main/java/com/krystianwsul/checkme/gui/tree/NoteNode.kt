package com.krystianwsul.checkme.gui.tree

import android.view.View
import com.krystianwsul.checkme.databinding.RowListBinding
import com.krystianwsul.checkme.gui.tree.checkable.CheckBoxState
import com.krystianwsul.checkme.gui.tree.invisible_checkbox.InvisibleCheckboxDelegate
import com.krystianwsul.checkme.gui.tree.invisible_checkbox.InvisibleCheckboxModelNode
import com.krystianwsul.checkme.gui.tree.multiline.MultiLineDelegate
import com.krystianwsul.checkme.gui.tree.multiline.MultiLineModelNode
import com.krystianwsul.checkme.gui.tree.multiline.MultiLineNameData
import com.krystianwsul.checkme.gui.utils.SearchData
import com.krystianwsul.common.utils.normalized
import com.krystianwsul.treeadapter.ModelNode
import com.krystianwsul.treeadapter.NodeContainer
import com.krystianwsul.treeadapter.TreeNode

class NoteNode(
        val note: String,
        instance: Boolean,
        override val parentNode: ModelNode<AbstractHolder>?,
) : GroupHolderNode(0), InvisibleCheckboxModelNode<AbstractHolder>, MultiLineModelNode<AbstractHolder> {

    override lateinit var treeNode: TreeNode<AbstractHolder>
        private set

    private lateinit var nodeContainer: NodeContainer<AbstractHolder>

    override val nodeType = NodeType.NOTE

    override val id get() = Id(nodeContainer.id)

    data class Id(val id: Any)

    private val normalizedNote by lazy { note.normalized() }

    init {
        check(note.isNotEmpty())
    }

    fun initialize(nodeContainer: NodeContainer<AbstractHolder>): TreeNode<AbstractHolder> {
        this.nodeContainer = nodeContainer

        treeNode = TreeNode(this, nodeContainer, expanded = false, selected = false)
        treeNode.setChildTreeNodes(listOf())

        return treeNode
    }

    override val textSelectable = true

    override val name get() = MultiLineNameData.Visible(note, unlimitedLines = true)

    override val isVisibleDuringActionMode = false

    override val isSeparatorVisibleWhenNotExpanded = true

    override val delegates by lazy {
        listOf(
                InvisibleCheckboxDelegate(this),
                MultiLineDelegate(this)
        )
    }

    override val widthKey
        get() = MultiLineDelegate.WidthKey(
                indentation,
                checkBoxState.visibility == View.GONE,
                hasAvatar,
                thumbnail != null
        )

    override fun compareTo(other: ModelNode<AbstractHolder>) = if (other is AssignedNode) 1 else -1

    override val checkBoxInvisible = instance
    override val checkBoxState = if (instance) CheckBoxState.Invisible else CheckBoxState.Gone

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

    class Holder(
            override val baseAdapter: BaseAdapter,
            rowListBinding: RowListBinding,
    ) : RegularNodeHolder(rowListBinding)
}
