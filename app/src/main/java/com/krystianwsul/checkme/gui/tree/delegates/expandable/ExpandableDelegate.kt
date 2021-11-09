package com.krystianwsul.checkme.gui.tree.delegates.expandable

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.gui.tree.NodeDelegate
import com.krystianwsul.treeadapter.TreeNode

class ExpandableDelegate(private val treeNode: TreeNode<*>) : NodeDelegate {

    override val state get() = treeNode.run { State(expandVisible, isExpanded) }

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder) {
        (viewHolder as ExpandableHolder).apply {
            rowExpand.apply {
                isVisible = treeNode.expandVisible

                setContent { ExpandImage() }
            }

            rowExpandMargin.isVisible = !treeNode.expandVisible
        }
    }

    @Composable
    private fun ExpandImage() {
        val imageResource = if (treeNode.isExpanded)
            R.drawable.ic_expand_less_black_36dp
        else
            R.drawable.ic_expand_more_black_36dp

        Image(
            painter = painterResource(id = imageResource),
            contentDescription = null,
        )
    }

    data class State(private val expandVisible: Boolean, private val isExpanded: Boolean)
}