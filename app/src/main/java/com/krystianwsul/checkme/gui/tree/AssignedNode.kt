package com.krystianwsul.checkme.gui.tree

import android.view.View
import com.krystianwsul.checkme.databinding.RowListAssignedBinding
import com.krystianwsul.checkme.gui.tree.checkable.CheckBoxState
import com.krystianwsul.checkme.gui.tree.invisible_checkbox.InvisibleCheckboxDelegate
import com.krystianwsul.checkme.gui.tree.invisible_checkbox.InvisibleCheckboxHolder
import com.krystianwsul.checkme.gui.tree.invisible_checkbox.InvisibleCheckboxModelNode
import com.krystianwsul.checkme.gui.tree.multiline.MultiLineDelegate
import com.krystianwsul.checkme.gui.tree.multiline.MultiLineHolder
import com.krystianwsul.checkme.gui.tree.multiline.MultiLineModelNode
import com.krystianwsul.checkme.gui.tree.multiline.MultiLineNameData
import com.krystianwsul.treeadapter.ModelNode
import com.krystianwsul.treeadapter.NodeContainer
import com.krystianwsul.treeadapter.TreeNode

class AssignedNode(
        override val assignedTo: List<User>,
        instance: Boolean,
        override val parentNode: ModelNode<AbstractHolder>?,
) : GroupHolderNode(0), InvisibleCheckboxModelNode, MultiLineModelNode {

    override lateinit var treeNode: TreeNode<AbstractHolder>
        private set

    private lateinit var nodeContainer: NodeContainer<AbstractHolder>

    override val nodeType = NodeType.ASSIGNED

    override val id get() = Id(nodeContainer.id)

    data class Id(val id: Any)

    override val name = MultiLineNameData.Gone

    override val isSeparatorVisibleWhenNotExpanded = true

    override val isVisibleDuringActionMode = false

    override val delegates by lazy {
        listOf(
                InvisibleCheckboxDelegate(this),
                MultiLineDelegate(this)
        )
    }

    override val checkBoxInvisible = instance
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

    fun initialize(nodeContainer: NodeContainer<AbstractHolder>): TreeNode<AbstractHolder> {
        this.nodeContainer = nodeContainer

        treeNode = TreeNode(this, nodeContainer, expanded = false, selected = false)
        treeNode.setChildTreeNodes(listOf())

        return treeNode
    }

    override fun compareTo(other: ModelNode<AbstractHolder>) = -1

    override fun matches(filterCriteria: Any?) = false

    override fun canBeShownWithFilterCriteria(filterCriteria: Any?) = true

    data class User(val name: String, val photoUrl: String?)

    class Holder(
            override val baseAdapter: BaseAdapter,
            binding: RowListAssignedBinding,
    ) : AbstractHolder(binding.root), InvisibleCheckboxHolder, MultiLineHolder {

        override val rowContainer = binding.rowListAssignedContainer
        override val rowTextLayout = binding.rowListAssignedTextLayout
        override val rowName = binding.rowListAssignedName
        override val rowDetails = binding.rowListAssignedDetails
        override val rowChildren = binding.rowListAssignedChildren
        override val rowThumbnail = binding.rowListAssignedThumbnail
        override val rowExpand = binding.rowListAssignedExpand
        override val rowCheckBoxFrame = binding.rowListAssignedCheckboxInclude.rowCheckboxFrame
        override val rowCheckBox = binding.rowListAssignedCheckboxInclude.rowCheckbox
        override val rowMarginStart = binding.rowListAssignedMargin
        override val rowImage = binding.rowListAssignedImage
        override val rowBigImage = binding.rowListAssignedBigImage
        override val rowBigImageLayout = binding.rowListAssignedBigImageLayout
        override val rowSeparator = binding.rowListAssignedSeparator
        override val rowChipGroup = binding.rowListAssignedChipGroup
        override val rowMarginEnd = binding.rowListAssignedMarginEnd
    }
}
