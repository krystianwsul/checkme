package com.krystianwsul.checkme.gui.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.krystianwsul.checkme.utils.addOneShotGlobalLayoutListener

class BottomAnchor @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : View(context, attrs, defStyleAttr) {

    init {
        addOneShotGlobalLayoutListener {
            layoutParams = (layoutParams as CoordinatorLayout.LayoutParams).apply {
                anchorId = -1
                setMargins(x.toInt(), y.toInt(), 0, 0)
            }
        }
    }
}