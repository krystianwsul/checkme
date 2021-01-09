package com.krystianwsul.treeadapter

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.RecyclerView

class TreeRecyclerView : RecyclerView {

    private var adapter: TreeViewAdapter<*>? = null

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun setAdapter(adapter: Adapter<*>?) {
        check(adapter is TreeViewAdapter<*>)
        check(this.adapter == null)

        super.setAdapter(adapter)

        this.adapter = adapter
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
    }
}