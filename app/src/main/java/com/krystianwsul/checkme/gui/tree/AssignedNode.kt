package com.krystianwsul.checkme.gui.tree

import android.view.View
import com.krystianwsul.checkme.gui.tree.checkable.CheckBoxState
import com.krystianwsul.checkme.gui.tree.checkable.CheckableDelegate
import com.krystianwsul.checkme.gui.tree.checkable.CheckableModelNode
import com.krystianwsul.checkme.gui.tree.multiline.MultiLineDelegate
import com.krystianwsul.checkme.gui.tree.multiline.MultiLineModelNode
import com.krystianwsul.checkme.gui.tree.multiline.MultiLineNameData
import com.krystianwsul.treeadapter.ModelNode
import com.krystianwsul.treeadapter.NodeContainer
import com.krystianwsul.treeadapter.TreeNode

class AssignedNode(
        override val assignedTo: List<User>,
        instance: Boolean,
        override val parentNode: ModelNode<NodeHolder>?,
) : GroupHolderNode(0), CheckableModelNode<NodeHolder>, MultiLineModelNode<NodeHolder> {

    override lateinit var treeNode: TreeNode<NodeHolder>
        private set

    private lateinit var nodeContainer: NodeContainer<NodeHolder>

    override val nodeType = NodeType.ASSIGNED

    override val id get() = Id(nodeContainer.id)

    data class Id(val id: Any)

    override val name = MultiLineNameData.Gone

    override val isSeparatorVisibleWhenNotExpanded = true

    override val isVisibleDuringActionMode = false

    override val delegates by lazy { listOf(CheckableDelegate(this), MultiLineDelegate(this)) }

    override val checkBoxState = if (instance) CheckBoxState.Invisible else CheckBoxState.Gone

    override val widthKey
        get() = MultiLineDelegate.WidthKey(
                indentation,
                checkBoxState.visibility == View.GONE,
                hasAvatar,
                thumbnail != null
        )

    init {
        check(assignedTo.isNotEmpty())
    }

    fun initialize(nodeContainer: NodeContainer<NodeHolder>): TreeNode<NodeHolder> {
        this.nodeContainer = nodeContainer

        treeNode = TreeNode(this, nodeContainer, expanded = false, selected = false)
        treeNode.setChildTreeNodes(listOf())

        return treeNode
    }

    override fun compareTo(other: ModelNode<NodeHolder>) = -1

    override fun matches(filterCriteria: Any?) = false

    override fun canBeShownWithFilterCriteria(filterCriteria: Any?) = true

    data class User(val name: String, val photoUrl: String?)
}
