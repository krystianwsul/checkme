package com.krystianwsul.checkme.gui.tree.multiline

import androidx.recyclerview.widget.RecyclerView
import com.krystianwsul.treeadapter.ModelNode

interface MultiLineModelNode<T> : ModelNode<T> where T : RecyclerView.ViewHolder, T : MultiLineHolder {

    val name: MultiLineNameData

    val details: Pair<String, Int>? get() = null

    val children: Pair<String, Int>? get() = null

    val indentation: Int

    val textSelectable get() = false // todo delegate note

    val widthKey: MultiLineDelegate.WidthKey// todo delegate simplify for each subclass
}