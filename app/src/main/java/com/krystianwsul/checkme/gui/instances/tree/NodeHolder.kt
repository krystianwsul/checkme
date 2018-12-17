package com.krystianwsul.checkme.gui.instances.tree

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.row_list.view.*

class NodeHolder(view: View) : RecyclerView.ViewHolder(view) {

    val rowContainer = itemView.rowContainer!!
    val rowName = itemView.rowName!!
    val rowDetails = itemView.rowDetails!!
    val rowChildren = itemView.rowChildren!!
    val rowExpand = itemView.rowExpand!!
    val rowCheckBox = itemView.rowCheckbox!!
    val rowSeparator = itemView.rowSeparator!!
}