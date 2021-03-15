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

        if (isAttachedToWindow) this.adapter?.onRecyclerDetachedFromWindow()

        super.setAdapter(adapter)

        this.adapter = adapter

        if (isAttachedToWindow) adapter.onRecyclerAttachedToWindow()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        adapter?.onRecyclerAttachedToWindow()
    }

    override fun onDetachedFromWindow() {
        adapter?.onRecyclerDetachedFromWindow()

        super.onDetachedFromWindow()
    }

    fun fixDrag() = cleanupLayoutState(this)
}