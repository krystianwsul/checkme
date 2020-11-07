package com.krystianwsul.checkme.gui.instances.tree

import com.krystianwsul.checkme.gui.utils.SearchData
import com.krystianwsul.common.utils.normalized
import com.krystianwsul.treeadapter.ModelNode
import com.krystianwsul.treeadapter.NodeContainer
import com.krystianwsul.treeadapter.TreeNode

class NoteNode(
        private val note: String,
        instance: Boolean,
        override val parentNode: ModelNode<NodeHolder>?
) : GroupHolderNode(0) {

    override lateinit var treeNode: TreeNode<NodeHolder>
        private set

    private lateinit var nodeContainer: NodeContainer<NodeHolder>

    override val id get() = Id(nodeContainer.id)

    data class Id(val id: Any)

    private val normalizedNote by lazy { note.normalized() }

    init {
        check(note.isNotEmpty())
    }

    fun initialize(nodeContainer: NodeContainer<NodeHolder>): TreeNode<NodeHolder> {
        this.nodeContainer = nodeContainer

        treeNode = TreeNode(this, nodeContainer, expanded = false, selected = false)
        treeNode.setChildTreeNodes(listOf())

        return treeNode
    }

    override val textSelectable = true

    override val name get() = NameData(note, unlimitedLines = true)

    override val isVisibleDuringActionMode = false

    override val isSeparatorVisibleWhenNotExpanded = true

    override fun compareTo(other: ModelNode<NodeHolder>) = -1

    override val checkBoxState = if (instance) CheckBoxState.Invisible else CheckBoxState.Gone

    override fun normalize() {
        normalizedNote
    }

    override fun filter(filterCriteria: Any?): Boolean {
        if (filterCriteria == null) return true

        val query = (filterCriteria as SearchData).query

        if (query.isEmpty()) return true

        return normalizedNote.contains(query)
    }
}
