package com.krystianwsul.checkme.gui.tree.delegates.singleline

import androidx.recyclerview.widget.RecyclerView
import com.krystianwsul.checkme.gui.tree.NodeDelegate

class SingleLineDelegate(private val modelNode: SingleLineModelNode) : NodeDelegate {

    override val state get() = State(modelNode.text)

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder) {
        (viewHolder as SingleLineHolder).rowName.text = modelNode.text
    }

    data class State(val text: String)
}