package com.krystianwsul.checkme.gui.tree.delegates.invisible_checkbox

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.krystianwsul.checkme.gui.tree.NodeDelegate

class InvisibleCheckboxDelegate(private val modelNode: InvisibleCheckboxModelNode) : NodeDelegate {

    override val state get() = State(modelNode.checkBoxInvisible)

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder) {
        (viewHolder as InvisibleCheckboxHolder).rowCheckBoxFrame.visibility =
                if (modelNode.checkBoxInvisible) View.INVISIBLE else View.GONE
    }

    data class State(val checkBoxInvisible: Boolean)
}