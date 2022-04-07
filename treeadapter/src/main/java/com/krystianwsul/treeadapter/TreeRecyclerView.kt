package com.krystianwsul.treeadapter

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.TreeLinearLayoutManager

class TreeRecyclerView : RecyclerView {


    private var adapter: TreeViewAdapter<*>? = null

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private val treeLinearLayoutManager = TreeLinearLayoutManager(context).also(::setLayoutManager)
    private val preventSettingLayoutManager = true

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

    override fun getLayoutManager() = treeLinearLayoutManager

    override fun setLayoutManager(layout: LayoutManager?) {
        if (preventSettingLayoutManager) throw UnsupportedOperationException()

        super.setLayoutManager(layout)
    }

    fun freezeTopPosition() = treeLinearLayoutManager.freezeTopPosition()
}