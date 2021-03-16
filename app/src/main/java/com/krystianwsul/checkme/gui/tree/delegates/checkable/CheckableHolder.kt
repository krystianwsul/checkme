package com.krystianwsul.checkme.gui.tree.delegates.checkable

import android.view.View
import android.widget.CheckBox
import android.widget.FrameLayout
import com.jakewharton.rxbinding4.view.clicks
import com.krystianwsul.checkme.gui.tree.BaseHolder
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.merge

interface CheckableHolder : BaseHolder {

    val rowCheckBoxFrame: FrameLayout
    val rowCheckBox: CheckBox
    val rowMarginStart: View

    override fun startRx() {
        listOf(
                rowCheckBoxFrame.clicks().doOnNext { rowCheckBox.toggle() },
                rowCheckBox.clicks()
        ).merge()
                .mapTreeNode()
                .switchMapCompletable {
                    ((it.modelNode as CheckableModelNode).checkBoxState as? CheckBoxState.Visible)?.completable
                            ?.invoke()
                            ?: Completable.complete()
                }
                .subscribe()
                .addTo(compositeDisposable)
    }
}