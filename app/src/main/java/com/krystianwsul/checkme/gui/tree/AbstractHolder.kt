package com.krystianwsul.checkme.gui.tree

import android.view.View
import com.jakewharton.rxbinding4.view.clicks
import com.jakewharton.rxbinding4.view.longClicks
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.TooltipManager
import com.krystianwsul.checkme.TooltipManager.subscribeShowBalloon
import com.krystianwsul.treeadapter.TreeHolder
import com.skydoves.balloon.ArrowOrientation
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

        TooltipManager.fiveSecondDelay()
                .filter { getTreeNode()?.modelNode?.isSelectable == true }
                .subscribeShowBalloon(
                        rowSeparator.context,
                    TooltipManager.Type.PRESS_TO_SELECT,
                    {
                        setTextResource(R.string.tooltip_press_to_select)
                        setArrowOrientation(ArrowOrientation.TOP)
                        setArrowPosition(0.1f)
                    },
                    { showAlignBottom(itemView) }
                )
            .addTo(compositeDisposable)
    }

    override fun stopRx() {
        super.stopRx()

        bindDisposable.clear()
    }
}