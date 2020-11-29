package com.krystianwsul.checkme.gui.tree

import android.view.ViewGroup
import com.krystianwsul.treeadapter.TreeModelAdapter
import com.krystianwsul.treeadapter.TreeNodeCollection

abstract class BaseAdapter : TreeModelAdapter<AbstractHolder> {

    protected abstract val treeNodeCollection: TreeNodeCollection<AbstractHolder>

    fun getNode(adapterPosition: Int) =
            treeNodeCollection.getNode(adapterPosition).let { it to it.modelNode as GroupHolderNode }

    final override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            NodeType.values()[viewType].onCreateViewHolder(this, parent)

    final override fun onViewAttachedToWindow(holder: AbstractHolder) = holder.onViewAttachedToWindow()

    final override fun onViewDetachedFromWindow(holder: AbstractHolder) = holder.onViewDetachedFromWindow()
}