package com.krystianwsul.checkme.gui.instances.tree

import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.checkme.gui.tree.AbstractHolder
import com.krystianwsul.checkme.gui.tree.delegates.multiline.MultiLineRow
import com.krystianwsul.treeadapter.TreeNode

class InstanceNodeDelegate(private val instanceData: GroupListDataWrapper.InstanceData) {

    val name = MultiLineRow.Visible(
        instanceData.name,
        if (instanceData.taskCurrent) R.color.textPrimary else R.color.textDisabled,
    )

    val details = instanceData.displayText
        .takeUnless { it.isNullOrEmpty() }
        ?.let { MultiLineRow.Visible(it, if (instanceData.taskCurrent) R.color.textSecondary else R.color.textDisabled) }

    fun getChildren(treeNode: TreeNode<AbstractHolder>): MultiLineRow? {
        if (!treeNode.isExpanded) return null

        val text = treeNode.allChildren
            .filter { it.modelNode is NotDoneGroupNode && it.canBeShown() }
            .map { it.modelNode as NotDoneGroupNode }
            .takeIf { it.isNotEmpty() }
            ?.sorted()
            ?.joinToString(", ") { it.singleInstanceData.name }
            ?: instanceData.note.takeIf { !it.isNullOrEmpty() }

        return text?.let {
            MultiLineRow.Visible(it, if (instanceData.taskCurrent) R.color.textSecondary else R.color.textDisabled)
        }
    }
}