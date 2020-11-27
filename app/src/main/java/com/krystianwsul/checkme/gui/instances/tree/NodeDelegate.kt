package com.krystianwsul.checkme.gui.instances.tree

import androidx.recyclerview.widget.RecyclerView

interface NodeDelegate {

    fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder)
}