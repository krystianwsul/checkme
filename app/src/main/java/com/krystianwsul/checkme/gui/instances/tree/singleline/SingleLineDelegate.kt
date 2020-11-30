package com.krystianwsul.checkme.gui.instances.tree.singleline

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.krystianwsul.checkme.gui.tree.GroupHolderNode
import com.krystianwsul.checkme.gui.tree.NodeDelegate

class SingleLineDelegate(private val modelNode: SingleLineModelNode) : NodeDelegate {

    override val state get() = State(modelNode.text)

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder) {
        (viewHolder as SingleLineHolder).apply {
            rowName.run {
                visibility = View.VISIBLE // todo delegate
                text = modelNode.text
                setTextColor(GroupHolderNode.colorPrimary) // todo delegate
            }
        }
    }

    data class State(val text: String)
}