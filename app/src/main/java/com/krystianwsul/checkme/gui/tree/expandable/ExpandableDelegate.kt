package com.krystianwsul.checkme.gui.tree.expandable

import androidx.core.view.isInvisible
import androidx.recyclerview.widget.RecyclerView
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.gui.tree.NodeDelegate
import com.krystianwsul.treeadapter.TreeNode

class ExpandableDelegate(private val treeNode: TreeNode<*>) : NodeDelegate {

    override val state get() = treeNode.run { State(expandVisible, isExpanded) }

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder) {
        (viewHolder as ExpandableHolder).rowExpand.run {
            isInvisible = !treeNode.expandVisible

            setImageResource(
                    if (treeNode.isExpanded)
                        R.drawable.ic_expand_less_black_36dp
                    else
                        R.drawable.ic_expand_more_black_36dp
            )
        }
    }

    data class State(private val expandVisible: Boolean, private val isExpanded: Boolean)
}