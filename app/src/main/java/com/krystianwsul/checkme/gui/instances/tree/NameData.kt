package com.krystianwsul.checkme.gui.instances.tree

import androidx.annotation.ColorInt

data class NameData(
        val text: String,
        @ColorInt val color: Int = GroupHolderNode.colorPrimary,
        val unlimitedLines: Boolean = false
)