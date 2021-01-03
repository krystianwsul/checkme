package com.krystianwsul.checkme.gui.tree

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.jakewharton.rxbinding3.view.clicks
import com.jakewharton.rxbinding3.view.longClicks
import com.krystianwsul.checkme.TooltipManager
import com.skydoves.balloon.ArrowOrientation
import com.skydoves.balloon.Balloon
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import java.util.concurrent.TimeUnit

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

        var balloon: Balloon? = null

        Observable.just(Unit)
                .mergeWith(Observable.never()) // hacky way to get dispose to get called on fragment destroyed
                .delay(5, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .mapTreeNode()
                .doOnDispose { balloon?.dismiss() }
                .subscribeBy {
                    if (it.modelNode.isSelectable) {
                        balloon = TooltipManager.tryCreateBalloon(
                                rowSeparator.context,
                                TooltipManager.Type.PRESS_TO_SELECT
                        ) {
                            setText("Press and hold to select this item.") // todo tooltip resource
                            setArrowOrientation(ArrowOrientation.TOP)
                        }?.apply { showAlignBottom(itemView) }
                    }
                }
                .addTo(compositeDisposable)
    }

    override fun onViewDetachedFromWindow() {
        compositeDisposable.clear()

        super.onViewDetachedFromWindow()
    }
}