package com.krystianwsul.checkme.gui.tree

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable

interface BaseHolder {

    val compositeDisposable: CompositeDisposable
    val bindDisposable: CompositeDisposable

    val baseAdapter: BaseAdapter
    val holderPosition: Int

    fun getTreeNode() = if (holderPosition >= 0) baseAdapter.getTreeNode(holderPosition) else null

    fun Observable<*>.mapTreeNode() = filter { holderPosition >= 0 }.map { baseAdapter.getTreeNode(holderPosition) }
    fun Single<*>.mapTreeNode() = filter { holderPosition >= 0 }.map { baseAdapter.getTreeNode(holderPosition) }

    fun startRx()
    fun stopRx()
}