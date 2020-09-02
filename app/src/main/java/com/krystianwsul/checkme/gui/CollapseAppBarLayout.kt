package com.krystianwsul.checkme.gui

import android.animation.ValueAnimator
import android.content.Context
import android.text.StaticLayout
import android.util.AttributeSet
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.updateLayoutParams
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.internal.CollapsingTextHelper
import com.krystianwsul.checkme.utils.addOneShotGlobalLayoutListener
import com.krystianwsul.checkme.utils.dpToPx
import com.krystianwsul.checkme.utils.getPrivateField
import kotlinx.android.synthetic.main.toolbar_collapse.view.*

class CollapseAppBarLayout : AppBarLayout {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private var valueAnimator: ValueAnimator? = null

    fun setText(title: String, text: String?, paddingLayout: View) {
        toolbarCollapseLayout.title = title

        val showText = text.isNullOrEmpty()

        toolbarCollapseText.also {
            it.visibility = if (showText) View.INVISIBLE else View.VISIBLE
            it.text = text

            it.addOneShotGlobalLayoutListener {
                val collapsingTextHelper: CollapsingTextHelper = toolbarCollapseLayout.getPrivateField("collapsingTextHelper")
                val textLayout: StaticLayout = collapsingTextHelper.getPrivateField("textLayout")

                val newHeight = textLayout.height + context.dpToPx(35).toInt() + it.height

                valueAnimator?.cancel()

                valueAnimator = ValueAnimator.ofInt(height, newHeight).apply {
                    addUpdateListener {
                        val currentHeight = it.animatedValue as Int

                        updateLayoutParams<CoordinatorLayout.LayoutParams> {
                            height = currentHeight
                        }

                        paddingLayout.setPadding(0, 0, 0, currentHeight)
                    }

                    start()
                }
            }
        }
    }
}