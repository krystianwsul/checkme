package com.krystianwsul.checkme.gui.tree

import androidx.recyclerview.widget.RecyclerView

interface NodeDelegate {

    val state: Any

    fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder)
}