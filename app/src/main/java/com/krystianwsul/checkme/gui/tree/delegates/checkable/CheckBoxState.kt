package com.krystianwsul.checkme.gui.tree.delegates.checkable

import android.view.View
import io.reactivex.rxjava3.core.Completable

sealed class CheckBoxState {

    abstract val visibility: Int
    open val checked: Boolean = false

    object Gone : CheckBoxState() {

        override val visibility = View.GONE
    }

    object Invisible : CheckBoxState() {

        override val visibility = View.INVISIBLE
    }

    class Visible(override val checked: Boolean, val completable: () -> Completable) : CheckBoxState() {

        override val visibility = View.VISIBLE

        override fun hashCode() = (if (checked) 1 else 0) + 31 * visibility

        override fun equals(other: Any?): Boolean {
            if (other === null) return false
            if (other === this) return true

            if (other !is Visible) return false

            if (other.checked != checked) return false

            return true
        }
    }
}