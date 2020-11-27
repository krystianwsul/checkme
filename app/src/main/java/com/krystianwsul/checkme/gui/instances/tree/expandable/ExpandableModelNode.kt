package com.krystianwsul.checkme.gui.instances.tree.expandable

import androidx.recyclerview.widget.RecyclerView
import com.krystianwsul.treeadapter.ModelNode

interface ExpandableModelNode<T> : ModelNode<T> where T : RecyclerView.ViewHolder, T : ExpandableHolder {
}