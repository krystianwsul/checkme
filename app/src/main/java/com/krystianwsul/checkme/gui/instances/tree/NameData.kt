package com.krystianwsul.checkme.gui.instances.tree

import androidx.annotation.ColorInt

sealed class NameData {

    open val unlimitedLines = false

    data class Visible(
            val text: String,
            @ColorInt val color: Int = GroupHolderNode.colorPrimary,
            override val unlimitedLines: Boolean = false,
    ) : NameData()

    object Invisible : NameData()

    object Gone : NameData()
}