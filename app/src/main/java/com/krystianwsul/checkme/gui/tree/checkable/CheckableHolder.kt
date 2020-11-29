package com.krystianwsul.checkme.gui.tree.checkable

import android.widget.CheckBox
import android.widget.FrameLayout
import com.jakewharton.rxbinding3.view.clicks
import com.krystianwsul.checkme.gui.tree.BaseHolder
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.merge

interface CheckableHolder : BaseHolder {

    val rowCheckBoxFrame: FrameLayout
    val rowCheckBox: CheckBox

    override fun onViewAttachedToWindow() {
        listOf(
                rowCheckBoxFrame.clicks().doOnNext { rowCheckBox.toggle() },
                rowCheckBox.clicks()
        ).merge()
                .mapNodes()
                .subscribe { (_, groupHolderNode) -> // todo delegate checkable move to delegate
                    ((groupHolderNode as CheckableModelNode<*>).checkBoxState as? CheckBoxState.Visible)?.listener?.invoke()
                }
                .addTo(compositeDisposable)
    }
}