package com.krystianwsul.checkme.gui.instances.tree

import androidx.recyclerview.widget.RecyclerView

interface NodeDelegate { // todo delegate generic (if possible?)

    val state: Any

    fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder)
}