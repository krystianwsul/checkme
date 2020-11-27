package com.krystianwsul.checkme.gui.instances.tree

import android.view.View
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.ChipGroup
import com.krystianwsul.checkme.gui.instances.tree.expandable.ExpandableHolder
import io.reactivex.disposables.CompositeDisposable

abstract class NodeHolder(view: View) : RecyclerView.ViewHolder(view), ExpandableHolder {

    abstract val rowContainer: LinearLayout
    abstract val rowTextLayout: LinearLayout
    abstract val rowName: TextView
    abstract val rowDetails: TextView
    abstract val rowChildren: TextView
    abstract val rowThumbnail: ImageView
    abstract val rowCheckBoxFrame: FrameLayout
    abstract val rowCheckBox: CheckBox
    abstract val rowMargin: View
    abstract val rowImage: ImageView?
    abstract val rowBigImage: ImageView?
    abstract val rowBigImageLayout: RelativeLayout?
    abstract val rowSeparator: View
    abstract val rowChipGroup: ChipGroup
    abstract val rowMarginEnd: View?

    val compositeDisposable = CompositeDisposable()
}