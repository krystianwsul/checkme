package com.krystianwsul.checkme.gui.instances.tree

import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.row_list.view.*

class NodeHolder(view: View) : RecyclerView.ViewHolder(view) {

    val rowContainer = itemView.rowContainer!!
    val rowTextLayout = itemView.rowTextLayout!!
    val rowName = itemView.rowName!!
    val rowDetails = itemView.rowDetails!!
    val rowChildren = itemView.rowChildren!!
    val rowThumbnail = itemView.rowThumbnail!!
    val rowExpand = itemView.rowExpand!!
    val rowCheckBoxFrame = itemView.rowCheckboxFrame!!
    val rowCheckBox = itemView.rowCheckbox!!
    val rowMargin = itemView.rowMargin!!
    val rowImage: ImageView? = itemView.rowImage
    val rowBigImage: ImageView? = itemView.rowBigImage
    val rowBigImageLayout: RelativeLayout? = itemView.rowBigImageLayout
    val rowSeparator = itemView.rowSeparator!!

    val compositeDisposable = CompositeDisposable()
}