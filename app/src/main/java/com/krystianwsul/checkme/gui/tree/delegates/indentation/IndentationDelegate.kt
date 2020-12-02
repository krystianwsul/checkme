package com.krystianwsul.checkme.gui.tree.delegates.indentation

import androidx.recyclerview.widget.RecyclerView
import com.krystianwsul.checkme.gui.tree.NodeDelegate
import com.krystianwsul.checkme.utils.setIndent

class IndentationDelegate(private val modelNode: IndentationModelNode) : NodeDelegate {

    override val state get() = State(modelNode.indentation)

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder) {
        (viewHolder as IndentationHolder).rowContainer.setIndent(modelNode.indentation)
    }

    data class State(val indentation: Int)
}