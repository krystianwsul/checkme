package com.krystianwsul.checkme.gui.instances.tree

import android.view.View
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.row_list.view.*

class NodeHolder(view: View) : RecyclerView.ViewHolder(view) {

    val rowContainer = itemView.rowContainer!!
    val rowTextLayout = itemView.rowTextLayout!!
    val rowName = itemView.rowName!!
    val rowDetails = itemView.rowDetails!!
    val rowChildren = itemView.rowChildren!!
    val rowExpand = itemView.rowExpand!!
    val rowCheckBox = itemView.rowCheckbox!!
    val rowMargin = itemView.rowMargin!!
    val rowImage: ImageView? = itemView.rowImage
    val rowBigImage = itemView.rowBigImage!!
    val rowSeparator = itemView.rowSeparator!!

    var textWidth: Int? = null
}