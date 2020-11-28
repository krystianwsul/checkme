package com.krystianwsul.checkme.gui.tree

import android.view.View
import com.krystianwsul.checkme.databinding.RowListBinding
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
        override val parentNode: ModelNode<BaseHolder>?,
) : GroupHolderNode(0), MultiLineModelNode<BaseHolder> {

    override lateinit var treeNode: TreeNode<BaseHolder>
        private set

    private lateinit var nodeContainer: NodeContainer<BaseHolder>

    override val nodeType = NodeType.IMAGE

    override val id get() = Id(nodeContainer.id)

    data class Id(val id: Any)

    override val name = MultiLineNameData.Gone

    override val isSeparatorVisibleWhenNotExpanded = true

    override val delegates by lazy { listOf(MultiLineDelegate(this)) }

    override val widthKey
        get() = MultiLineDelegate.WidthKey(
                indentation,
                checkBoxState.visibility == View.GONE,
                hasAvatar,
                thumbnail != null
        )

    fun initialize(nodeContainer: NodeContainer<BaseHolder>): TreeNode<BaseHolder> {
        this.nodeContainer = nodeContainer
        treeNode = TreeNode(this, nodeContainer, expanded = false, selected = false)

        treeNode.setChildTreeNodes(ArrayList())
        return treeNode
    }

    override fun compareTo(other: ModelNode<BaseHolder>) = when (other) {
        is AssignedNode -> 1
        is NoteNode -> 1
        else -> -1
    }

    override fun onClick(holder: BaseHolder) = showImage(holder.rowBigImage!!, imageData)

    override fun matches(filterCriteria: Any?) = false

    override fun canBeShownWithFilterCriteria(filterCriteria: Any?) = true

    class ImageData(
            val imageState: ImageState,
            val onImageShown: (StfalconImageViewer<ImageState>) -> Unit,
            val onDismiss: () -> Unit,
            val showImage: Boolean,
    )

    class Holder(rowListBinding: RowListBinding) : RegularNodeHolder(rowListBinding)
}