package com.krystianwsul.checkme.gui.tree

import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.ChipGroup
import com.jakewharton.rxbinding3.view.clicks
import com.jakewharton.rxbinding3.view.longClicks
import com.krystianwsul.checkme.gui.instances.tree.singleline.SingleLineHolder
import com.krystianwsul.checkme.gui.tree.avatar.AvatarHolder
import com.krystianwsul.checkme.gui.tree.checkable.CheckBoxState
import com.krystianwsul.checkme.gui.tree.checkable.CheckableHolder
import com.krystianwsul.checkme.gui.tree.checkable.CheckableModelNode
import com.krystianwsul.checkme.gui.tree.expandable.ExpandableHolder
import com.krystianwsul.checkme.gui.tree.multiline.MultiLineHolder
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.merge

abstract class AbstractHolder(view: View) :
        RecyclerView.ViewHolder(view),
        BaseHolder,
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

    override val holderPosition get() = adapterPosition

    override fun onViewAttachedToWindow() {
        super<ExpandableHolder>.onViewAttachedToWindow()

        itemView.clicks()
                .mapNodes()
                .subscribe { (treeNode, _) -> treeNode.onClick(this) }
                .addTo(compositeDisposable)

        itemView.longClicks { true }
                .mapNodes()
                .subscribe { (_, groupHolderNode) -> groupHolderNode.onLongClick(this) }
                .addTo(compositeDisposable)

        /* todo delegate consider moving this into the other classes.  Maybe publish observables in the holder,
        subscribe in the delegate, and add a disposable to the holder that gets cleared when something binds to it?
         */
        listOf(
                rowCheckBoxFrame.clicks().doOnNext { rowCheckBox.toggle() },
                rowCheckBox.clicks()
        ).merge()
                .mapNodes()
                .subscribe { (_, groupHolderNode) ->
                    ((groupHolderNode as? CheckableModelNode<*>)?.checkBoxState as? CheckBoxState.Visible)?.listener?.invoke()
                }
                .addTo(compositeDisposable)
    }
}