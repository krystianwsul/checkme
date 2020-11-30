package com.krystianwsul.checkme.gui.tree

import android.view.View
import com.krystianwsul.checkme.databinding.RowListImageBinding
import com.krystianwsul.checkme.gui.tree.multiline.MultiLineDelegate
import com.krystianwsul.checkme.gui.tree.multiline.MultiLineModelNode
import com.krystianwsul.checkme.gui.tree.multiline.MultiLineNameData
import com.krystianwsul.common.firebase.models.ImageState
import com.krystianwsul.treeadapter.ModelNode
import com.krystianwsul.treeadapter.NodeContainer
import com.krystianwsul.treeadapter.TreeNode
import com.stfalcon.imageviewer.StfalconImageViewer
import java.util.*

class ImageNode(
        override val imageData: ImageData,
        override val parentNode: ModelNode<AbstractHolder>?,
) : GroupHolderNode(0), MultiLineModelNode<AbstractHolder> {

    override lateinit var treeNode: TreeNode<AbstractHolder>
        private set

    private lateinit var nodeContainer: NodeContainer<AbstractHolder>

    override val nodeType = NodeType.IMAGE

    override val id get() = Id(nodeContainer.id)

    data class Id(val id: Any)

    override val name = MultiLineNameData.Gone

    override val isSeparatorVisibleWhenNotExpanded = true

    override val widthKey
        get() = MultiLineDelegate.WidthKey(
                indentation,
                checkBoxState.visibility == View.GONE,
                hasAvatar,
                thumbnail != null
        )

    fun initialize(nodeContainer: NodeContainer<AbstractHolder>): TreeNode<AbstractHolder> {
        this.nodeContainer = nodeContainer
        treeNode = TreeNode(this, nodeContainer, expanded = false, selected = false)

        treeNode.setChildTreeNodes(ArrayList())
        return treeNode
    }

    override fun compareTo(other: ModelNode<AbstractHolder>) = when (other) {
        is AssignedNode -> 1
        is NoteNode -> 1
        else -> -1
    }

    override fun onClick(holder: AbstractHolder) = showImage(holder.rowBigImage!!, imageData)

    override fun matches(filterCriteria: Any?) = false

    override fun canBeShownWithFilterCriteria(filterCriteria: Any?) = true

    class ImageData(
            val imageState: ImageState,
            val onImageShown: (StfalconImageViewer<ImageState>) -> Unit,
            val onDismiss: () -> Unit,
            val showImage: Boolean,
    )

    class Holder(
            override val baseAdapter: BaseAdapter,
            binding: RowListImageBinding,
    ) : AbstractHolder(binding.root) {

        override val rowContainer = binding.rowContainer
        override val rowTextLayout = binding.rowTextLayout
        override val rowName = binding.rowName
        override val rowDetails = binding.rowDetails
        override val rowChildren = binding.rowChildren
        override val rowThumbnail = binding.rowThumbnail
        override val rowExpand = binding.rowExpand
        override val rowCheckBoxFrame = binding.rowListCheckboxInclude.rowCheckboxFrame
        override val rowCheckBox = binding.rowListCheckboxInclude.rowCheckbox
        override val rowMarginStart = binding.rowMargin
        override val rowImage = binding.rowImage
        override val rowBigImage = binding.rowBigImage
        override val rowBigImageLayout = binding.rowBigImageLayout
        override val rowSeparator = binding.rowSeparator
        override val rowChipGroup = binding.rowChipGroup
        override val rowMarginEnd = binding.rowMarginEnd
    }
}