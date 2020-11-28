package com.krystianwsul.checkme.gui.tree.checkable

import androidx.recyclerview.widget.RecyclerView
import com.krystianwsul.treeadapter.ModelNode

interface CheckableModelNode<T> : ModelNode<T> where T : RecyclerView.ViewHolder, T : CheckableHolder {

    val checkBoxState: CheckBoxState
}