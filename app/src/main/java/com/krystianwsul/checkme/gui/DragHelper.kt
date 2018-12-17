package com.krystianwsul.checkme.gui

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.treeadapter.TreeViewAdapter

class DragHelper(private val callback: MyCallback) : ItemTouchHelper(callback) {

    override fun startDrag(viewHolder: RecyclerView.ViewHolder) {
        MyCrashlytics.logMethod(this, "startPosition before: " + callback.startPosition)
        check(callback.startPosition == null)
        check(callback.endPosition == null)

        callback.startPosition = viewHolder.adapterPosition
        MyCrashlytics.logMethod(this, "startPosition after: " + callback.startPosition)

        super.startDrag(viewHolder)
    }

    abstract class MyCallback : SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {

        var startPosition: Int? = null

        var endPosition: Int? = null
            private set

        override fun isLongPressDragEnabled() = false

        override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
            MyCrashlytics.logMethod(this, "endPosition before: $endPosition")

            val from = viewHolder.adapterPosition
            MyCrashlytics.logMethod(this, "from: $from")
            endPosition = target.adapterPosition

            MyCrashlytics.logMethod(this, "endPosition after: $endPosition")

            getTreeViewAdapter().updateDisplayedNodes {
                getTreeViewAdapter().moveItem(from, endPosition!!, TreeViewAdapter.Placeholder)
            }

            return true
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit

        override fun isItemViewSwipeEnabled() = false

        override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
            MyCrashlytics.logMethod(this, "endPosition: $endPosition")

            endPosition?.let {
                checkNotNull(startPosition)

                if (startPosition != endPosition) {
                    getTreeViewAdapter().setNewItemPosition(it)
                    onSetNewItemPosition()
                }
            }

            startPosition = null
            endPosition = null

            super.clearView(recyclerView, viewHolder)
        }

        abstract fun getTreeViewAdapter(): TreeViewAdapter
        abstract fun onSetNewItemPosition()
    }
}