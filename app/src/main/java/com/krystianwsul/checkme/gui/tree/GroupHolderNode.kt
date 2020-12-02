package com.krystianwsul.checkme.gui.tree

import android.view.View
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.utils.setIndent
import com.krystianwsul.common.firebase.models.ImageState
import com.krystianwsul.treeadapter.ModelNode
import com.krystianwsul.treeadapter.ModelState
import com.krystianwsul.treeadapter.TreeNode

abstract class GroupHolderNode : ModelNode<AbstractHolder> {

    companion object {

        fun getColor(@ColorRes color: Int) = ContextCompat.getColor(MyApplication.instance, color)

        val colorPrimary by lazy { getColor(R.color.textPrimary) }
        val colorSecondary by lazy { getColor(R.color.textSecondary) }
        val colorDisabled by lazy { getColor(R.color.textDisabled) }
        val colorSelected by lazy { getColor(R.color.selected) }
        val colorBackground by lazy { getColor(R.color.materialBackground) }
    }

    protected abstract val treeNode: TreeNode<AbstractHolder>

    abstract val holderType: HolderType

    final override val itemViewType by lazy { holderType.ordinal }

    protected open val ripple = false

    protected open val imageData: ImageNode.ImageData? = null // todo delegate image

    protected open val delegates = listOf<NodeDelegate>()

    open val indentation: Int = 0

    override val state: ModelState
        get() = State(
                id,
                indentation,
                imageData?.imageState,
                delegates.map { it.state }
        )

    protected open val colorBackground = Companion.colorBackground

    data class State(
            override val id: Any,
            val indentation: Int,
            val imageState: ImageState?,
            val delegateStates: List<Any>,
    ) : ModelState

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, startingDrag: Boolean) {
        val groupHolder = viewHolder as AbstractHolder

        groupHolder.run {
            val taskImage = imageData

            if (taskImage == null) {
                rowContainer.visibility = View.VISIBLE
                rowContainer.setIndent(indentation)

                delegates.forEach { it.onBindViewHolder(viewHolder) }

                itemView.run {
                    setBackgroundColor(if (treeNode.isSelected && !(isPressed && startingDrag)) colorSelected else colorBackground)

                    foreground = if (ripple && !isPressed) ContextCompat.getDrawable(context, R.drawable.item_background_material) else null
                }
            }

            rowSeparator.visibility = if (treeNode.separatorVisible) View.VISIBLE else View.INVISIBLE
        }
    }
}
