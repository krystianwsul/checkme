package com.krystianwsul.checkme.gui.tree.delegates.expandable

import android.view.View
import android.widget.ImageView
import com.jakewharton.rxbinding4.view.clicks
import com.krystianwsul.checkme.gui.tree.BaseHolder
import io.reactivex.rxjava3.kotlin.addTo

interface ExpandableHolder : BaseHolder {

    val rowExpand: ImageView
    val rowExpandMargin: View

    override fun startRx() {
        rowExpand.clicks()
                .mapTreeNode()
                .subscribe { it.onExpandClick() }
                .addTo(compositeDisposable)
    }
}