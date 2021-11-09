package com.krystianwsul.checkme.gui.tree.delegates.expandable

import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.krystianwsul.checkme.gui.tree.NodeDelegate
import com.krystianwsul.treeadapter.TreeNode

class ExpandableDelegate(private val treeNode: TreeNode<*>) : NodeDelegate {

    override val state get() = treeNode.run { State(expandVisible, isExpanded) }

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder) {
        (viewHolder as ExpandableHolder).apply {
            rowExpand.apply {
                isVisible = treeNode.expandVisible

                setContent { ExpandImage(treeNode.isExpanded) }
            }

            rowExpandMargin.isVisible = !treeNode.expandVisible
        }
    }

    @Composable
    private fun ExpandImage(isExpanded: Boolean) {
        Icon(
            imageVector = Icons.Filled.run { if (isExpanded) ExpandLess else ExpandMore },
            contentDescription = null,
            Modifier.width(36.dp),
        )
    }

    data class State(private val expandVisible: Boolean, private val isExpanded: Boolean)
}