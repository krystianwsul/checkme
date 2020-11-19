package com.krystianwsul.checkme.gui.instances.tree

import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import kotlinx.android.synthetic.main.row_list_dialog.view.*

class DialogNodeHolder(view: View) : NodeHolder(view) {

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
    override val rowImage: ImageView? = null
    override val rowBigImage: ImageView? = null
    override val rowBigImageLayout: RelativeLayout? = null
    override val rowSeparator = itemView.rowSeparator!!
}