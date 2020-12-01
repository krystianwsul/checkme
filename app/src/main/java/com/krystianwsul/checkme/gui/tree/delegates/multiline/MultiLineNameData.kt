package com.krystianwsul.checkme.gui.tree.delegates.multiline

import androidx.annotation.ColorInt
import com.krystianwsul.checkme.gui.tree.GroupHolderNode

sealed class MultiLineNameData {

    open val unlimitedLines = false

    data class Visible(
            val text: String,
            @ColorInt val color: Int = GroupHolderNode.colorPrimary,
            override val unlimitedLines: Boolean = false,
    ) : MultiLineNameData()

    object Invisible : MultiLineNameData()

    object Gone : MultiLineNameData()
}