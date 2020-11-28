package com.krystianwsul.checkme.gui.tree

import android.app.Activity
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.databinding.RowAssignedChipBinding
import com.krystianwsul.checkme.domainmodel.toImageLoader
import com.krystianwsul.checkme.gui.instances.list.GroupListFragment
import com.krystianwsul.checkme.gui.tree.avatar.AvatarDelegate
import com.krystianwsul.checkme.gui.tree.checkable.CheckBoxState
import com.krystianwsul.checkme.gui.tree.checkable.CheckableDelegate
import com.krystianwsul.checkme.gui.tree.expandable.ExpandableDelegate
import com.krystianwsul.checkme.gui.tree.multiline.MultiLineDelegate
import com.krystianwsul.checkme.utils.isLandscape
import com.krystianwsul.checkme.utils.loadPhoto
import com.krystianwsul.checkme.utils.setIndent
import com.krystianwsul.common.firebase.models.ImageState
import com.krystianwsul.treeadapter.ModelNode
import com.krystianwsul.treeadapter.ModelState
import com.krystianwsul.treeadapter.TreeNode
import com.stfalcon.imageviewer.StfalconImageViewer

abstract class GroupHolderNode(val indentation: Int) : ModelNode<NodeHolder> {

    companion object {

        fun getColor(@ColorRes color: Int) = ContextCompat.getColor(MyApplication.instance, color)

        val colorPrimary by lazy { getColor(R.color.textPrimary) }
        val colorSecondary by lazy { getColor(R.color.textSecondary) }
        val colorDisabled by lazy { getColor(R.color.textDisabled) }
        val colorSelected by lazy { getColor(R.color.selected) }
        val colorBackground by lazy { getColor(R.color.materialBackground) }
    }

    protected abstract val treeNode: TreeNode<NodeHolder>

    protected open val hasAvatar = false // todo delegate
    open val checkBoxState: CheckBoxState = CheckBoxState.Gone // todo delegate

    open fun onLongClick(viewHolder: RecyclerView.ViewHolder) = treeNode.onLongClick()

    final override val itemViewType = GroupListFragment.GroupAdapter.TYPE_GROUP

    protected open val ripple = false

    protected open val imageData: ImageNode.ImageData? = null // todo delegate image

    protected open val assignedTo: List<AssignedNode.User> = listOf() // todo delegate assigned

    protected open val thumbnail: ImageState? = null

    protected open val delegates = listOf<NodeDelegate>()

    final override val state
        get() = State(
                id,
                indentation,
                imageData?.imageState,
                assignedTo,
                thumbnail,
                delegates.map { it.state }
        )

    protected open val colorBackground = Companion.colorBackground

    data class State(
            val id: Any,
            val indentation: Int,
            val imageState: ImageState?,
            val assignedTo: List<AssignedNode.User>,
            val thumbnail: ImageState?,
            val delegateStates: List<Any>,
    ) : ModelState {

        override fun same(other: ModelState) = (other as State).id == id
    }

    protected fun showImage(rowBigImage: ImageView, taskImage: ImageNode.ImageData) {
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

    final override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, startingDrag: Boolean) {
        val groupHolder = viewHolder as NodeHolder

        groupHolder.run {
            val taskImage = imageData

            if (taskImage != null) {
                rowContainer.visibility = View.GONE
                rowBigImageLayout!!.visibility = View.VISIBLE
                rowChipGroup.isVisible = false

                taskImage.imageState
                        .toImageLoader()
                        .load(rowBigImage!!)

                if (taskImage.showImage) showImage(rowBigImage!!, taskImage)
            } else {
                rowContainer.visibility = View.VISIBLE
                rowBigImageLayout?.visibility = View.GONE

                rowContainer.setIndent(indentation)

                rowThumbnail.apply {
                    if (thumbnail != null) {
                        visibility = View.VISIBLE

                        thumbnail!!.toImageLoader().load(this, true)
                    } else {
                        visibility = View.GONE
                    }
                }

                delegates.forEach { it.onBindViewHolder(viewHolder) }

                // todo delegate remove these
                if (delegates.none { it is ExpandableDelegate<*> }) {
                    rowExpand.isGone = true
                    rowMarginEnd!!.isVisible = true
                }
                if (delegates.none { it is AvatarDelegate<*> }) rowImage?.isVisible = false
                if (delegates.none { it is CheckableDelegate<*> }) rowCheckBoxFrame.isGone = true
                if (delegates.none { it is MultiLineDelegate<*> }) {
                    rowDetails.isGone = true
                    rowChildren.isGone = true
                }

                rowMarginStart.isVisible = checkBoxState.visibility == View.GONE && !hasAvatar

                itemView.run {
                    setBackgroundColor(if (treeNode.isSelected && !(isPressed && startingDrag)) colorSelected else colorBackground)

                    foreground = if (ripple && !isPressed) ContextCompat.getDrawable(context, R.drawable.item_background_material) else null
                }

                if (assignedTo.isEmpty()) {
                    rowChipGroup.isVisible = false
                } else {
                    rowChipGroup.isVisible = true

                    rowChipGroup.removeAllViews()

                    assignedTo.forEach { user ->
                        RowAssignedChipBinding.inflate(
                                LayoutInflater.from(rowContainer.context),
                                rowChipGroup,
                                true
                        )
                                .root
                                .apply {
                                    text = user.name
                                    loadPhoto(user.photoUrl)
                                    isCloseIconVisible = false
                                }
                    }
                }
            }

            rowSeparator.visibility = if (treeNode.separatorVisible) View.VISIBLE else View.INVISIBLE
        }
    }
}
