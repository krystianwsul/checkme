package com.krystianwsul.checkme.gui.tree.delegates.multiline

interface MultiLineModelNode {

    val name: MultiLineNameData

    val details: Pair<String, Int>? get() = null

    val children: Pair<String, Int>? get() = null

    val widthKey: MultiLineDelegate.WidthKey// todo delegate simplify for each subclass
}