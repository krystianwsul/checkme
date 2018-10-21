package com.krystianwsul.checkme.gui

import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import com.krystianwsul.treeadapter.TreeViewAdapter

class DragHelper(private val treeViewAdapter: TreeViewAdapter, private val callback: MyCallback = MyCallback(treeViewAdapter)) : ItemTouchHelper(callback) {

    private var startPosition: Int? = null

    init {
        callback.listener = { endPosition ->
            checkNotNull(startPosition)

            if (startPosition != endPosition)
                treeViewAdapter.setNewItemPosition(endPosition)

            startPosition = null
        }
    }

    override fun startDrag(viewHolder: RecyclerView.ViewHolder) {
        check(startPosition == null)
        check(callback.endPosition == null)

        startPosition = viewHolder.adapterPosition

        super.startDrag(viewHolder)
    }

    class MyCallback(private val treeViewAdapter: TreeViewAdapter) : SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {

        var endPosition: Int? = null
            private set

        lateinit var listener: (Int) -> Unit

        override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
            val from = viewHolder.adapterPosition
            endPosition = target.adapterPosition

            treeViewAdapter.moveItem(from, endPosition!!)

            return true
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit

        override fun isItemViewSwipeEnabled() = false

        override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
            endPosition?.let { listener(it) }
            endPosition = null

            super.clearView(recyclerView, viewHolder)
        }
    }
}