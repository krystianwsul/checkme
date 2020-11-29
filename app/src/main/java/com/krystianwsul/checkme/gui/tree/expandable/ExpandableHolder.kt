package com.krystianwsul.checkme.gui.tree.expandable

import android.widget.ImageView
import com.jakewharton.rxbinding3.view.clicks
import com.krystianwsul.checkme.gui.tree.BaseHolder
import io.reactivex.rxkotlin.addTo

interface ExpandableHolder : BaseHolder {

    val rowExpand: ImageView

    override fun onViewAttachedToWindow() {
        super.onViewAttachedToWindow()

        rowExpand.clicks()
                .mapTreeNode()
                .subscribe { it.onExpandClick() }
                .addTo(compositeDisposable)
    }
}