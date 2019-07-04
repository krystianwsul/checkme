package com.krystianwsul.checkme.gui.tasks

import android.content.Context
import android.util.AttributeSet
import android.view.WindowInsets
import android.widget.FrameLayout

class InsetLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : FrameLayout(context, attrs, defStyleAttr) {

    init {
        fitsSystemWindows = true
    }

    @Suppress("DEPRECATION")
    override fun onApplyWindowInsets(insets: WindowInsets) = super.onApplyWindowInsets(insets.replaceSystemWindowInsets(0, insets.systemWindowInsetTop, 0, insets.systemWindowInsetBottom))!!
}