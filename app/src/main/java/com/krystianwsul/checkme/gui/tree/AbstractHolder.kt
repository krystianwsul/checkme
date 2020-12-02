package com.krystianwsul.checkme.gui.tree

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.jakewharton.rxbinding3.view.clicks
import com.jakewharton.rxbinding3.view.longClicks
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo

abstract class AbstractHolder(view: View) : RecyclerView.ViewHolder(view), BaseHolder {

    abstract val rowSeparator: View

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