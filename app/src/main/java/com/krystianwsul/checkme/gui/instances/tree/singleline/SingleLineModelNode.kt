package com.krystianwsul.checkme.gui.instances.tree.singleline

import androidx.recyclerview.widget.RecyclerView
import com.krystianwsul.treeadapter.ModelNode

interface SingleLineModelNode<T> : ModelNode<T> where T : RecyclerView.ViewHolder, T : SingleLineHolder {

    val text: String
}