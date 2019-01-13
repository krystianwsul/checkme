package com.krystianwsul.checkme.gui

import android.animation.ValueAnimator
import android.graphics.Canvas
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.utils.dpToPx
import com.krystianwsul.treeadapter.Sortable
import com.krystianwsul.treeadapter.TreeViewAdapter

abstract class DragHelper(callback: MyCallback = MyCallback()) : ItemTouchHelper(callback) {

    companion object {

        private val animationTime by lazy {
            MyApplication.instance
                    .resources
                    .getInteger(android.R.integer.config_shortAnimTime)
                    .toLong()
        }
    }

    init {
        @Suppress("LeakingThis")
        callback.dragHelper = this
    }

    private var startPosition: Int? = null
    private var endPosition: Int? = null

    private var valueAnimator: ValueAnimator? = null

    override fun startDrag(viewHolder: RecyclerView.ViewHolder) {
        MyCrashlytics.logMethod(this, "startPosition before: $startPosition")
        check(startPosition == null)
        check(endPosition == null)

        startPosition = viewHolder.adapterPosition
        MyCrashlytics.logMethod(this, "startPosition after: $startPosition")

        super.startDrag(viewHolder)
    }

    private fun onMoveHelper(viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
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

    private fun clearViewHelper(viewHolder: RecyclerView.ViewHolder) {
        MyCrashlytics.logMethod(this, "endPosition: $endPosition")

        endPosition?.let {
            checkNotNull(startPosition)

            if (startPosition != endPosition) {
                getTreeViewAdapter().setNewItemPosition(it)
                onSetNewItemPosition()
            }
        }

        getTreeViewAdapter().notifyItemChanged(viewHolder.adapterPosition)

        startPosition = null
        endPosition = null
    }

    abstract fun getTreeViewAdapter(): TreeViewAdapter
    abstract fun onSetNewItemPosition()

    private fun canDropOverHelper(recyclerView: RecyclerView, target: RecyclerView.ViewHolder): Boolean {
        val treeNodeCollection = (recyclerView.adapter as TreeViewAdapter).getTreeNodeCollection()

        val position = target.adapterPosition.let { if (it == treeNodeCollection.displayedSize) it - 1 else it }

        return treeNodeCollection.getNode(position).modelNode is Sortable
    }

    private fun onChildDrawHelper(viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float, isCurrentlyActive: Boolean) {
        viewHolder.itemView.apply {
            translationX = dX
            translationY = dY

            valueAnimator?.cancel()

            if (isCurrentlyActive) {
                elevation = context.dpToPx(6)
            } else {
                valueAnimator = ValueAnimator.ofFloat(elevation, 0f).apply {
                    duration = animationTime
                    addUpdateListener { elevation = it.animatedValue as Float }
                    start()
                }
            }
        }
    }

    class MyCallback : SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {

        lateinit var dragHelper: DragHelper

        override fun isLongPressDragEnabled() = false

        override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) = dragHelper.onMoveHelper(viewHolder, target)

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit

        override fun isItemViewSwipeEnabled() = false

        override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
            dragHelper.clearViewHelper(viewHolder)

            super.clearView(recyclerView, viewHolder)
        }

        override fun canDropOver(recyclerView: RecyclerView, current: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) = dragHelper.canDropOverHelper(recyclerView, target)

        override fun onChildDraw(c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) = dragHelper.onChildDrawHelper(viewHolder, dX, dY, isCurrentlyActive)
    }
}