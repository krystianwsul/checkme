package com.krystianwsul.checkme.gui.tree

import android.view.View
import com.jakewharton.rxbinding4.view.clicks
import com.jakewharton.rxbinding4.view.longClicks
import com.krystianwsul.treeadapter.TreeHolder
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo

abstract class AbstractHolder(view: View) : TreeHolder(view), BaseHolder {

    final override val bindDisposable = CompositeDisposable()

    abstract val rowSeparator: View

    override val holderPosition get() = adapterPosition

    override fun startRx() {
        itemView.clicks()
            .mapTreeNode()
            .subscribe { it.onClick(this) }
            .addTo(compositeDisposable)

        itemView.longClicks { true }
            .mapTreeNode()
            .subscribe { it.onLongClick(this) }
            .addTo(compositeDisposable)
    }

    override fun stopRx() {
        super.stopRx()

        bindDisposable.clear()
    }
}