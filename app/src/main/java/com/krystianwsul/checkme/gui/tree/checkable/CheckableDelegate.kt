package com.krystianwsul.checkme.gui.tree.checkable

import androidx.recyclerview.widget.RecyclerView
import com.krystianwsul.checkme.gui.tree.NodeDelegate

class CheckableDelegate<T>(private val checkableModelNode: CheckableModelNode<T>) : NodeDelegate
        where T : RecyclerView.ViewHolder,
              T : CheckableHolder {

    override val state get() = State(checkableModelNode.checkBoxState)

    /*
    todo delegate some of the classes Models that use this could probably do without, or with a simplified version.
    Consider moving checkbox layout into separate file and including it.
    Set fixed visibility if possible and skip the delegate, and maybe introduce a second delegate for the simpler
    Gone/Invisible scenario like with AssignedNode.
     */

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder) {
        val checkBoxState = checkableModelNode.checkBoxState

        (viewHolder as CheckableHolder).apply {
            rowCheckBoxFrame.visibility = checkBoxState.visibility
            rowCheckBox.isChecked = checkBoxState.checked
        }
    }

    data class State(val checkBoxState: CheckBoxState)
}