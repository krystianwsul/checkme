package com.krystianwsul.checkme.gui.tree.delegates.multiline

import androidx.annotation.ColorInt
import com.krystianwsul.checkme.gui.tree.GroupHolderNode

sealed class MultiLineNameData {

    data class Visible(
            val text: String,
            @ColorInt val color: Int = GroupHolderNode.colorPrimary,
    ) : MultiLineNameData()

    object Invisible : MultiLineNameData()
}