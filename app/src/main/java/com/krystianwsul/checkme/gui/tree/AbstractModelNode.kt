package com.krystianwsul.checkme.gui.tree

import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import androidx.recyclerview.widget.RecyclerView
import com.krystianwsul.checkme.R
import com.krystianwsul.treeadapter.ModelNode
import com.krystianwsul.treeadapter.ModelState
import com.krystianwsul.treeadapter.TreeNode

abstract class AbstractModelNode : ModelNode<AbstractHolder> {

    protected abstract val treeNode: TreeNode<AbstractHolder>

    abstract val holderType: HolderType

    final override val itemViewType by lazy { holderType.ordinal }

    protected open val disableRipple = false

    protected open val delegates = listOf<NodeDelegate>()

    override val state: ModelState get() = State(id, delegates.map { it.state })

    data class State(override val id: Any, val delegateStates: List<Any>) : ModelState

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, startingDrag: Boolean) {
        (viewHolder as AbstractHolder).apply {
            bindDisposable.clear()

            delegates.forEach { it.onBindViewHolder(viewHolder) }

            itemView.apply {
                setBackgroundColor(
                    ContextCompat.getColor(
                        context,
                        if (treeNode.isSelected && !(isPressed && startingDrag))
                            R.color.selected
                        else
                            R.color.materialBackground
                    ))

                foreground = if (!disableRipple && !isPressed)
                    ContextCompat.getDrawable(context, R.drawable.item_background_material)
                else
                    null
            }

            onPayload(viewHolder)
        }
    }

    override fun forceSelected(viewHolder: RecyclerView.ViewHolder) {
        viewHolder.itemView.apply { setBackgroundColor(ContextCompat.getColor(context, R.color.selected)) }
    }

    open val wantsSeparator: Boolean? = null

    override fun onPayload(viewHolder: RecyclerView.ViewHolder) {
        if (wantsSeparator != null) {
            val x = 1 / 2
        }

        val separatorVisible = treeNode.separatorVisible

        wantsSeparator?.let { check(it == separatorVisible) }

        (viewHolder as AbstractHolder).rowSeparator.isInvisible = !separatorVisible
    }
}
