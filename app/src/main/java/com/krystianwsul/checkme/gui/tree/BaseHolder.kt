package com.krystianwsul.checkme.gui.tree

import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable

interface BaseHolder {

    val compositeDisposable: CompositeDisposable

    val baseAdapter: BaseAdapter
    val holderPosition: Int

    fun getTreeNode() = if (holderPosition >= 0) baseAdapter.getTreeNode(holderPosition) else null

    fun Observable<*>.mapTreeNode() = filter { holderPosition >= 0 }.map { baseAdapter.getTreeNode(holderPosition) }!!
    fun Single<*>.mapTreeNode() = filter { holderPosition >= 0 }.map { baseAdapter.getTreeNode(holderPosition) }

    fun onViewAttachedToWindow() = Unit

    fun onViewDetachedFromWindow() = compositeDisposable.clear()
}