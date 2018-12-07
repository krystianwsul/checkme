package com.krystianwsul.checkme.gui

import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.treeadapter.TreeViewAdapter

class DragHelper(
        private val treeViewAdapter: TreeViewAdapter,
        private val callback: MyCallback = MyCallback(treeViewAdapter)) : ItemTouchHelper(callback) {

    override fun startDrag(viewHolder: RecyclerView.ViewHolder) {
        MyCrashlytics.logMethod(this, "startPosition before: " + callback.startPosition)
        check(callback.startPosition == null)
        check(callback.endPosition == null)

        callback.startPosition = viewHolder.adapterPosition
        MyCrashlytics.logMethod(this, "startPosition after: " + callback.startPosition)

        super.startDrag(viewHolder)
    }

    open class MyCallback(private val treeViewAdapter: TreeViewAdapter) : SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {

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

            treeViewAdapter.updateDisplayedNodes {
                treeViewAdapter.moveItem(from, endPosition!!, TreeViewAdapter.Placeholder)
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
                    treeViewAdapter.setNewItemPosition(it)
                    onSetNewItemPosition()
                }
            }

            startPosition = null
            endPosition = null

            super.clearView(recyclerView, viewHolder)
        }

        protected open fun getTreeViewAdapter(): TreeViewAdapter? = null
        protected open fun onSetNewItemPosition() = Unit
    }
}