package com.krystianwsul.checkme.gui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import com.krystianwsul.checkme.R
import kotlinx.android.synthetic.main.view_my_spinner.view.*

class MySpinner @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : FrameLayout(context, attrs, defStyleAttr) {

    init {
        View.inflate(context, R.layout.view_my_spinner, this)
    }

    val text by lazy { mySpinnerText!! }

    fun setSelection(position: Int) {
        text.setText(text.adapter.getItem(position).toString())
    }
}