package com.krystianwsul.checkme.gui.tree

import android.view.ViewGroup
import com.krystianwsul.treeadapter.TreeModelAdapter
import com.krystianwsul.treeadapter.TreeNodeCollection

abstract class BaseAdapter : TreeModelAdapter<AbstractHolder> {

    protected abstract val treeNodeCollection: TreeNodeCollection<AbstractHolder>

    fun getTreeNode(adapterPosition: Int) = treeNodeCollection.getNode(adapterPosition)

    final override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            HolderType.values()[viewType].onCreateViewHolder(this, parent)
}