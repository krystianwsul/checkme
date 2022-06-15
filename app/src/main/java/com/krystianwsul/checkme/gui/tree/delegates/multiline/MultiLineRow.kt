package com.krystianwsul.checkme.gui.tree.delegates.multiline

import androidx.annotation.ColorRes
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.R

sealed class MultiLineRow {

    data class Visible(
        val text: String,
        @ColorRes val colorId: Int = R.color.textPrimary,
    ) : MultiLineRow() {

        init {
            if (text.isBlank()) MyCrashlytics.logException(EmptyRowException())
        }

        private class EmptyRowException() : Exception()
    }

    object Invisible : MultiLineRow()
}