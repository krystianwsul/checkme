package com.krystianwsul.checkme.gui.instances.tree

import android.view.View
import kotlinx.android.synthetic.main.row_list.view.*

class RegularNodeHolder(view: View) : NodeHolder(view) {

    override val rowContainer = itemView.rowContainer!!
    override val rowTextLayout = itemView.rowTextLayout!!
    override val rowName = itemView.rowName!!
    override val rowDetails = itemView.rowDetails!!
    override val rowChildren = itemView.rowChildren!!
    override val rowThumbnail = itemView.rowThumbnail!!
    override val rowExpand = itemView.rowExpand!!
    override val rowCheckBoxFrame = itemView.rowCheckboxFrame!!
    override val rowCheckBox = itemView.rowCheckbox!!
    override val rowMargin = itemView.rowMargin!!
    override val rowImage = itemView.rowImage!!
    override val rowBigImage = itemView.rowBigImage!!
    override val rowBigImageLayout = itemView.rowBigImageLayout!!
    override val rowSeparator = itemView.rowSeparator!!
}