package com.krystianwsul.checkme.gui.instances.tree

import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.domainmodel.GroupTypeFactory
import com.krystianwsul.checkme.gui.tree.DetailsNode
import com.krystianwsul.checkme.gui.tree.delegates.multiline.MultiLineRow
import com.krystianwsul.treeadapter.TreeNode

class InstanceRowsDelegate(singleBridge: GroupTypeFactory.SingleBridge) :
    DetailsNode.ProjectRowsDelegate(
        singleBridge.projectInfo,
        if (singleBridge.instanceData.taskCurrent) R.color.textSecondary else R.color.textDisabled,
    ) {

    private val instanceData = singleBridge.instanceData

    private val name = MultiLineRow.Visible(
        instanceData.name,
        if (instanceData.taskCurrent) R.color.textPrimary else R.color.textDisabled,
    )

    private val details: MultiLineRow.Visible? = singleBridge.displayText.toSecondaryRow()

    private val note = instanceData.note.toSecondaryRow()

    override fun getRowsWithoutProject(isExpanded: Boolean, allChildren: List<TreeNode<*>>): List<MultiLineRow> {
        val children = allChildren.takeIf { !isExpanded }
            ?.filter { it.modelNode is NotDoneGroupNode && it.canBeShown() }
            ?.map { it.modelNode as NotDoneGroupNode }
            ?.takeIf { it.isNotEmpty() }
            ?.sorted()
            ?.joinToString(", ") { it.contentDelegate.name }
            .toSecondaryRow()

        return listOfNotNull(
            name,
            details,
            children,
            note.takeIf { !isExpanded },
        )
    }
}