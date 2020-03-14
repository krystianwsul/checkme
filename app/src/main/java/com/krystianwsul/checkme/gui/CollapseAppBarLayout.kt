package com.krystianwsul.checkme.gui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import com.google.android.material.appbar.AppBarLayout
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.utils.addOneShotGlobalLayoutListener
import kotlinx.android.synthetic.main.toolbar_collapse.view.*

class CollapseAppBarLayout : AppBarLayout {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    fun setText(title: String, text: String?) {
        toolbarCollapseLayout.title = title

        val showText = text.isNullOrEmpty()

        toolbarCollapseText.also {
            it.visibility = if (showText) View.GONE else View.VISIBLE
            it.text = text

            it.addOneShotGlobalLayoutListener {
                layoutParams = layoutParams.apply {
                    height = resources.getDimension(R.dimen.collapseAppBarHeight).toInt() + it.height
                }
            }
        }
    }
}