package com.krystianwsul.checkme.gui.instances.tree

import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.checkme.gui.tree.delegates.multiline.MultiLineModelNode
import com.krystianwsul.checkme.gui.tree.delegates.multiline.MultiLineRow
import com.krystianwsul.treeadapter.TreeNode

class InstanceRowsDelegate(
    private val instanceData: GroupListDataWrapper.InstanceData,
    showDetails: Boolean = true,
) : MultiLineModelNode.RowsDelegate {

    private val secondaryColor = if (instanceData.taskCurrent) R.color.textSecondary else R.color.textDisabled

    private val name = MultiLineRow.Visible(
        instanceData.name,
        if (instanceData.taskCurrent) R.color.textPrimary else R.color.textDisabled,
    )

    private fun String?.toSecondary() = takeIf { !it.isNullOrEmpty() }?.let { MultiLineRow.Visible(it, secondaryColor) }

    private val details: MultiLineRow.Visible? = instanceData.takeIf { showDetails }
        ?.displayText
        .toSecondary()

    private val note = instanceData.note.toSecondary()

    private val project = instanceData.projectInfo
        ?.name
        .toSecondary()

    override fun getRows(isExpanded: Boolean, allChildren: List<TreeNode<*>>): List<MultiLineRow> {
        val children = allChildren.takeIf { !isExpanded }
            ?.filter { it.modelNode is NotDoneGroupNode && it.canBeShown() }
            ?.map { it.modelNode as NotDoneGroupNode }
            ?.takeIf { it.isNotEmpty() }
            ?.sorted()
            ?.joinToString(", ") { it.singleInstanceData.name }
            .toSecondary()

        return listOfNotNull(
            name,
            details,
            children,
            note.takeIf { !isExpanded },
            project?.takeIf { !isExpanded },
        )
    }
}