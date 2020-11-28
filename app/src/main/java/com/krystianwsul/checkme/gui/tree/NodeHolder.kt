package com.krystianwsul.checkme.gui.tree

import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.ChipGroup
import com.krystianwsul.checkme.gui.instances.tree.singleline.SingleLineHolder
import com.krystianwsul.checkme.gui.tree.avatar.AvatarHolder
import com.krystianwsul.checkme.gui.tree.checkable.CheckableHolder
import com.krystianwsul.checkme.gui.tree.expandable.ExpandableHolder
import com.krystianwsul.checkme.gui.tree.multiline.MultiLineHolder
import io.reactivex.disposables.CompositeDisposable

abstract class NodeHolder(view: View) :
        RecyclerView.ViewHolder(view),
        ExpandableHolder,
        AvatarHolder,
        CheckableHolder,
        MultiLineHolder,
        SingleLineHolder {

    abstract val rowContainer: LinearLayout
    abstract val rowThumbnail: ImageView
    abstract val rowMarginStart: View
    abstract val rowBigImage: ImageView?
    abstract val rowBigImageLayout: RelativeLayout?
    abstract val rowSeparator: View
    abstract val rowChipGroup: ChipGroup
    abstract val rowMarginEnd: View?

    final override val compositeDisposable = CompositeDisposable()
}