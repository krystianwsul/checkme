package com.krystianwsul.checkme.gui.tree

import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.ChipGroup
import com.jakewharton.rxbinding3.view.clicks
import com.jakewharton.rxbinding3.view.longClicks
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo

abstract class AbstractHolder(view: View) : RecyclerView.ViewHolder(view), BaseHolder {

    abstract val rowContainer: LinearLayout
    abstract val rowThumbnail: ImageView
    abstract val rowMarginStart: View
    abstract val rowSeparator: View
    abstract val rowChipGroup: ChipGroup
    abstract val rowMarginEnd: View?

    final override val compositeDisposable = CompositeDisposable()

    override val holderPosition get() = adapterPosition

    override fun onViewAttachedToWindow() {
        itemView.clicks()
                .mapTreeNode()
                .subscribe { it.onClick(this) }
                .addTo(compositeDisposable)

        itemView.longClicks { true }
                .mapTreeNode()
                .subscribe { it.onLongClick(this) }
                .addTo(compositeDisposable)
    }
}