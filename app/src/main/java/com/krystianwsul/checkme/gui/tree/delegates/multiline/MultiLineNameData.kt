package com.krystianwsul.checkme.gui.tree.delegates.multiline

import androidx.annotation.ColorRes
import com.krystianwsul.checkme.R

sealed class MultiLineNameData {

    data class Visible(
            val text: String,
            @ColorRes val colorId: Int = R.color.textPrimary,
    ) : MultiLineNameData()

    object Invisible : MultiLineNameData()
}