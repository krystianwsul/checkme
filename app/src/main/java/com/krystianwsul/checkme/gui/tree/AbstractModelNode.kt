package com.krystianwsul.checkme.gui.tree

import android.view.View
import androidx.core.content.ContextCompat
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
            delegates.forEach { it.onBindViewHolder(viewHolder) }

            itemView.apply {
                setBackgroundColor(ContextCompat.getColor(
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

            onPayload(viewHolder, TreeNode.PayloadSeparator)
        }
    }

    override fun onPayload(viewHolder: RecyclerView.ViewHolder, payloadSeparator: TreeNode.PayloadSeparator) {
        (viewHolder as AbstractHolder).rowSeparator.visibility =
                if (treeNode.separatorVisible) View.VISIBLE else View.INVISIBLE
    }
}
