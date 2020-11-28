package com.krystianwsul.checkme.gui.instances.tree.singleline

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.krystianwsul.checkme.gui.instances.tree.GroupHolderNode
import com.krystianwsul.checkme.gui.instances.tree.NodeDelegate

class SingleLineDelegate<T>(private val singleLineModelNode: SingleLineModelNode<T>) : NodeDelegate
        where T : RecyclerView.ViewHolder,
              T : SingleLineHolder {

    override val state get() = State(singleLineModelNode.text)

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder) {
        (viewHolder as SingleLineHolder).apply {
            rowName.run {
                visibility = View.VISIBLE // todo delegate
                text = singleLineModelNode.text
                setTextColor(GroupHolderNode.colorPrimary) // todo delegate
            }
        }
    }

    data class State(val text: String)
}