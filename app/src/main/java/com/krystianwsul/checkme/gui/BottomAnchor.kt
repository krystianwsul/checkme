package com.krystianwsul.checkme.gui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.krystianwsul.checkme.utils.addOneShotGlobalLayoutListener
import com.krystianwsul.checkme.utils.dpToPx

class BottomAnchor @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : View(context, attrs, defStyleAttr) {

    init {
        addOneShotGlobalLayoutListener {
            layoutParams = (layoutParams as CoordinatorLayout.LayoutParams).apply {
                anchorId = -1
                setMargins(x.toInt(), (y - context.dpToPx(5)).toInt(), 0, 0) // todo probably won't need offset with new snackbar theme
            }
        }
    }
}