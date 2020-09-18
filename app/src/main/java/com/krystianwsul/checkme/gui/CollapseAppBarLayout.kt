package com.krystianwsul.checkme.gui

import android.animation.ValueAnimator
import android.content.Context
import android.text.StaticLayout
import android.util.AttributeSet
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.internal.CollapsingTextHelper
import com.krystianwsul.checkme.utils.addOneShotGlobalLayoutListener
import com.krystianwsul.checkme.utils.dpToPx
import com.krystianwsul.checkme.utils.getPrivateField
import kotlinx.android.synthetic.main.toolbar_collapse.view.*
import kotlin.math.abs

class CollapseAppBarLayout : AppBarLayout {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private var valueAnimator: ValueAnimator? = null

    private var initialHeight: Int? = null

    private lateinit var paddingLayout: View

    private val collapsingTextHelper: CollapsingTextHelper by lazy {
        toolbarCollapseLayout.getPrivateField("collapsingTextHelper")
    }

    private val textLayout: StaticLayout by lazy {
        collapsingTextHelper.getPrivateField("textLayout")
    }

    fun setText(title: String, text: String?, paddingLayout: View) {
        valueAnimator?.cancel()

        this.paddingLayout = paddingLayout

        toolbarCollapseLayout.title = title

        toolbarCollapseText.also {
            val hideText = text.isNullOrEmpty()

            it.isVisible = !hideText
            it.text = text

            it.addOneShotGlobalLayoutListener {
                if (initialHeight == null) initialHeight = it.height

                animateHeight(hideText)
            }
        }
    }

    private fun animateHeight(hideText: Boolean) {
        fun setNewHeight(newHeight: Int) {
            updateLayoutParams<CoordinatorLayout.LayoutParams> {
                height = newHeight
            }

            paddingLayout.setPadding(0, 0, 0, newHeight)
        }

        val newHeight = if (hideText) {
            toolbar.height + 1 // stupid hack because otherwise title doesn't show
        } else {
            initialHeight!! + context.dpToPx(35).toInt() + textLayout.height
        }

        if (abs(newHeight - height) <= 1) { // same stupid hack
            setNewHeight(newHeight)
        } else {
            valueAnimator = ValueAnimator.ofInt(height, newHeight).apply {
                addUpdateListener {
                    val currentHeight = it.animatedValue as Int

                    setNewHeight(currentHeight)
                }

                start()
            }
        }
    }

    fun hideText() {
        toolbarCollapseLayout.title = null
        toolbarCollapseText.isVisible = false

        animateHeight(true)
    }
}