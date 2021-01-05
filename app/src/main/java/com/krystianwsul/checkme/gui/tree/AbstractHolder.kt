package com.krystianwsul.checkme.gui.tree

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.jakewharton.rxbinding3.view.clicks
import com.jakewharton.rxbinding3.view.longClicks
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.TooltipManager
import com.krystianwsul.checkme.TooltipManager.subscribeShowBalloon
import com.skydoves.balloon.ArrowOrientation
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.plusAssign

abstract class AbstractHolder(view: View) : RecyclerView.ViewHolder(view), BaseHolder {

    abstract val rowSeparator: View

    final override val compositeDisposable by lazy {
        CompositeDisposable().also { baseAdapter.compositeDisposable += it }
    }

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

    override fun onViewDetachedFromWindow() {
        compositeDisposable.clear()

        super.onViewDetachedFromWindow()
    }
}