package com.krystianwsul.checkme.gui.tree.invisible_checkbox

import androidx.recyclerview.widget.RecyclerView
import com.krystianwsul.treeadapter.ModelNode

interface InvisibleCheckboxModelNode<T> : ModelNode<T> where T : RecyclerView.ViewHolder, T : InvisibleCheckboxHolder {

    val checkBoxInvisible: Boolean
}