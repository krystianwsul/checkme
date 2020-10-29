package com.krystianwsul.checkme.gui.instances.tree

import com.krystianwsul.checkme.gui.instances.list.GroupListFragment
import com.krystianwsul.common.firebase.models.ImageState
import com.krystianwsul.treeadapter.ModelNode
import com.krystianwsul.treeadapter.NodeContainer
import com.krystianwsul.treeadapter.TreeNode
import com.stfalcon.imageviewer.StfalconImageViewer
import java.util.*

class ImageNode(override val imageData: ImageData) : GroupHolderNode(0) {

    override lateinit var treeNode: TreeNode<NodeHolder>
        private set

    private lateinit var nodeContainer: NodeContainer<NodeHolder>

    override val id get() = Id(nodeContainer.id)

    data class Id(val id: Any)

    override val name: NameData? = null

    override val isSeparatorVisibleWhenNotExpanded = true

    override val itemViewType = GroupListFragment.GroupAdapter.TYPE_IMAGE

    fun initialize(nodeContainer: NodeContainer<NodeHolder>): TreeNode<NodeHolder> {
        this.nodeContainer = nodeContainer
        treeNode = TreeNode(this, nodeContainer, expanded = false, selected = false)

        treeNode.setChildTreeNodes(ArrayList())
        return treeNode
    }

    override fun compareTo(other: ModelNode<NodeHolder>) = if (other is NoteNode) 1 else -1

    override fun onClick(holder: NodeHolder) = showImage(holder.rowBigImage!!, imageData)

    override fun filter(filterCriteria: Any?) = filterCriteria == null

    class ImageData(
            val imageState: ImageState,
            val onImageShown: (StfalconImageViewer<ImageState>) -> Unit,
            val onDismiss: () -> Unit,
            val showImage: Boolean)
}