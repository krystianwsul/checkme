package com.krystianwsul.checkme.gui.tree.checkable

import androidx.recyclerview.widget.RecyclerView
import com.krystianwsul.checkme.gui.tree.NodeDelegate

class CheckableDelegate<T>(private val modelNode: CheckableModelNode<T>) : NodeDelegate
        where T : RecyclerView.ViewHolder,
              T : CheckableHolder {

    override val state get() = State(modelNode.checkBoxState)

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder) {
        val checkBoxState = modelNode.checkBoxState

        (viewHolder as CheckableHolder).apply {
            rowCheckBoxFrame.visibility = checkBoxState.visibility
            rowCheckBox.isChecked = checkBoxState.checked
        }
    }

    data class State(val checkBoxState: CheckBoxState)
}