package com.krystianwsul.checkme.gui.tree.delegates.expandable

import android.widget.ImageView
import com.jakewharton.rxbinding4.view.clicks
import com.krystianwsul.checkme.gui.tree.BaseHolder
import io.reactivex.rxjava3.kotlin.addTo

interface ExpandableHolder : BaseHolder {

    val rowExpand: ImageView

    override fun startRx() {
        rowExpand.clicks()
                .mapTreeNode()
                .subscribe { it.onExpandClick() }
                .addTo(compositeDisposable)
    }
}