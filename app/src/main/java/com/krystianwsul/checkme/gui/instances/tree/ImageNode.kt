package com.krystianwsul.checkme.gui.instances.tree

import com.krystianwsul.checkme.firebase.ImageState
import com.krystianwsul.treeadapter.ModelNode
import com.krystianwsul.treeadapter.NodeContainer
import com.krystianwsul.treeadapter.TreeNode
import com.stfalcon.imageviewer.StfalconImageViewer
import java.util.*

class ImageNode(override val imageData: ImageData) : GroupHolderNode(0) {

    override lateinit var treeNode: TreeNode
        private set

    private lateinit var nodeContainer: NodeContainer

    override val id get() = Id(nodeContainer.id)

    data class Id(val id: Any)

    override val name: Triple<String, Int, Boolean>? = null

    override val isSeparatorVisibleWhenNotExpanded = true

    override val ignoreStale = true

    override val itemViewType = GroupListFragment.GroupAdapter.TYPE_IMAGE

    fun initialize(nodeContainer: NodeContainer): TreeNode {
        this.nodeContainer = nodeContainer
        treeNode = TreeNode(this, nodeContainer, expanded = false, selected = false)

        treeNode.setChildTreeNodes(ArrayList())
        return treeNode
    }

    override fun compareTo(other: ModelNode) = if (other is NoteNode) 1 else -1

    class ImageData(
            val imageState: ImageState,
            val onImageShown: (StfalconImageViewer<ImageState>) -> Unit,
            val onDismiss: () -> Unit,
            val showImage: Boolean)
}