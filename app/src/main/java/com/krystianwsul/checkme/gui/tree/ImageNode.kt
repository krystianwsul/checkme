package com.krystianwsul.checkme.gui.tree

import android.app.Activity
import android.graphics.Color
import android.util.Log
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.databinding.RowListImageBinding
import com.krystianwsul.checkme.domainmodel.toImageLoader
import com.krystianwsul.checkme.utils.animateVisibility
import com.krystianwsul.checkme.utils.isLandscape
import com.krystianwsul.common.firebase.models.ImageState
import com.krystianwsul.treeadapter.ModelNode
import com.krystianwsul.treeadapter.ModelState
import com.krystianwsul.treeadapter.NodeContainer
import com.krystianwsul.treeadapter.TreeNode
import com.stfalcon.imageviewer.StfalconImageViewer
import java.util.*

class ImageNode(
        private val imageData: ImageData,
        override val parentNode: ModelNode<AbstractHolder>?,
) : AbstractModelNode() {

    override lateinit var treeNode: TreeNode<AbstractHolder>
        private set

    private lateinit var nodeContainer: NodeContainer<AbstractHolder>

    override val holderType = HolderType.IMAGE

    override val id get() = Id(nodeContainer.id)

    override val state get() = State(super.state, imageData.imageState)

    data class Id(val id: Any)

    override val isSeparatorVisibleWhenNotExpanded = true

    fun initialize(nodeContainer: NodeContainer<AbstractHolder>): TreeNode<AbstractHolder> {
        this.nodeContainer = nodeContainer
        treeNode = TreeNode(this, nodeContainer)

        treeNode.setChildTreeNodes(ArrayList())
        return treeNode
    }

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, startingDrag: Boolean) {
        super.onBindViewHolder(viewHolder, startingDrag)

        (viewHolder as Holder).apply {
            imageData.imageState
                    .toImageLoader()
                    .load(rowBigImage) { animateVisibility(listOf(), listOf(progress)) }

            if (imageData.showImage) showImage(rowBigImage, imageData)
        }
    }

    private fun showImage(rowBigImage: ImageView, taskImage: ImageData) {
        val activity = rowBigImage.context as Activity

        fun setStatusColor(@ColorInt color: Int) {
            activity.window.statusBarColor = color
        }

        val fixStatusBar = !activity.resources.isLandscape

        val viewer = StfalconImageViewer.Builder(
                rowBigImage.context,
                listOf(taskImage.imageState)
        ) { view, image -> image.toImageLoader().load(view) }
                .withTransitionFrom(rowBigImage)
                .withDismissListener {
                    taskImage.onDismiss()

                    if (fixStatusBar)
                        setStatusColor(ContextCompat.getColor(rowBigImage.context, R.color.primaryDarkColor))
                }.apply {
                    if (fixStatusBar) withHiddenStatusBar(false)
                }
                .show()

        if (fixStatusBar) setStatusColor(Color.BLACK)

        taskImage.onImageShown(viewer)
    }

    override fun compareTo(other: ModelNode<AbstractHolder>) = when (other) {
        is DetailsNode -> 1
        else -> -1
    }

    override fun onClick(holder: AbstractHolder) = showImage((holder as Holder).rowBigImage, imageData)

    class ImageData(
            val imageState: ImageState,
            val onImageShown: (StfalconImageViewer<ImageState>) -> Unit,
            val onDismiss: () -> Unit,
            val showImage: Boolean,
    )

    data class State(val superState: ModelState, val imageState: ImageState) : ModelState {

        override val id = superState.id
    }

    class Holder(
            override val baseAdapter: BaseAdapter,
            binding: RowListImageBinding,
    ) : AbstractHolder(binding.root) {

        val progress = binding.rowListImageProgress
        val rowBigImage = binding.rowListImageBigImage
        override val rowSeparator = binding.rowListImageSeparator
    }
}