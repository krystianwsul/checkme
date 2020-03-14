package com.krystianwsul.checkme.gui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import com.google.android.material.appbar.AppBarLayout
import kotlinx.android.synthetic.main.toolbar_collapse.view.*

class CollapseAppBarLayout : AppBarLayout {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    fun setTitle(title: String) {
        toolbarCollapseLayout.title = title
    }

    fun setText(text: String?) {
        toolbarCollapseText.also {
            it.visibility = if (text.isNullOrEmpty()) View.GONE else View.VISIBLE
            it.text = text
        }
    }
}