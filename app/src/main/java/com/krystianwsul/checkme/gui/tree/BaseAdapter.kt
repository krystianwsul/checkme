package com.krystianwsul.checkme.gui.tree

import android.view.ViewGroup
import com.krystianwsul.treeadapter.TreeModelAdapter
import com.krystianwsul.treeadapter.TreeNodeCollection
import io.reactivex.disposables.CompositeDisposable

abstract class BaseAdapter : TreeModelAdapter<AbstractHolder> {

    abstract val compositeDisposable: CompositeDisposable

    protected abstract val treeNodeCollection: TreeNodeCollection<AbstractHolder>

    fun getTreeNode(adapterPosition: Int) = treeNodeCollection.getNode(adapterPosition)

    final override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            HolderType.values()[viewType].onCreateViewHolder(this, parent)

    final override fun onViewAttachedToWindow(holder: AbstractHolder) = holder.onViewAttachedToWindow()

    final override fun onViewDetachedFromWindow(holder: AbstractHolder) = holder.onViewDetachedFromWindow()
}