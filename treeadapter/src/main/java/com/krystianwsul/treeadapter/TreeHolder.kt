package com.krystianwsul.treeadapter

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.rxjava3.disposables.CompositeDisposable

abstract class TreeHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    val compositeDisposable = CompositeDisposable()

    open fun startRx() = Unit

    fun stopRx() = compositeDisposable.clear()
}