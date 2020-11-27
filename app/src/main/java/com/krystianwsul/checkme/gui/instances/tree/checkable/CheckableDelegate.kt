package com.krystianwsul.checkme.gui.instances.tree.checkable

import androidx.recyclerview.widget.RecyclerView
import com.krystianwsul.checkme.gui.instances.tree.NodeDelegate

class CheckableDelegate<T>(private val checkableModelNode: CheckableModelNode<T>) : NodeDelegate
        where T : RecyclerView.ViewHolder,
              T : CheckableHolder {

    override val state get() = State(checkableModelNode.checkBoxState)

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder) {
        val checkBoxState = checkableModelNode.checkBoxState

        (viewHolder as CheckableHolder).apply {
            rowCheckBoxFrame.visibility = checkBoxState.visibility
            rowCheckBox.isChecked = checkBoxState.checked
        }
    }

    data class State(val checkBoxState: CheckBoxState)
}