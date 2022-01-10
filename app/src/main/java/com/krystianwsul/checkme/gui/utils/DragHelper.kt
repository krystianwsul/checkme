package com.krystianwsul.checkme.gui.utils

import android.animation.ValueAnimator
import android.graphics.Canvas
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.gui.tree.AbstractHolder
import com.krystianwsul.checkme.utils.dpToPx
import com.krystianwsul.treeadapter.PositionMode
import com.krystianwsul.treeadapter.Sortable
import com.krystianwsul.treeadapter.TreeRecyclerView
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

        fixDrag()
    }

    private fun fixDrag() {
        /**
         * This fixes a glitch in which moving certain children prevents the next drag&drop from working (viewHolder
         * detaches and moves, but never calls canDropOver)
         */
        recyclerView.post { recyclerView.fixDrag() }
    }

    protected abstract val recyclerView: TreeRecyclerView

    private fun onMoveHelper(viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
        MyCrashlytics.logMethod(this, "endPosition before: $endPosition")

        val from = viewHolder.adapterPosition
        check(from >= 0)

        MyCrashlytics.logMethod(this, "from: $from")
        endPosition = target.adapterPosition

        MyCrashlytics.logMethod(this, "endPosition after: $endPosition")

        getTreeViewAdapter().updateDisplayedNodes(true) { getTreeViewAdapter().moveItem(from, endPosition!!, it) }

        fixDrag()

        return true
    }

    private fun clearViewHelper(viewHolder: RecyclerView.ViewHolder) {
        MyCrashlytics.logMethod(this, "endPosition: $endPosition")

        if (endPosition != null) {
            checkNotNull(startPosition)

            if (startPosition != endPosition) getTreeViewAdapter().setNewItemPosition(endPosition!!)

            getTreeViewAdapter().getTreeNodeCollection()
                .getNode(viewHolder.adapterPosition, PositionMode.DISPLAYED)
                .onBindViewHolder(viewHolder)
        } else {
            startPosition?.let { getTreeViewAdapter().selectNode(it) }
        }

        startPosition = null
        endPosition = null
    }

    abstract fun getTreeViewAdapter(): TreeViewAdapter<AbstractHolder>

    private fun canDropOverHelper(target: RecyclerView.ViewHolder): Boolean {
        val treeNodeCollection = getTreeViewAdapter().getTreeNodeCollection()

        val position = target.adapterPosition.let { if (it == treeNodeCollection.displayedNodes.size) it - 1 else it }

        val thisTreeNode = treeNodeCollection.getNode(startPosition!!)
        val thisSortable = thisTreeNode.modelNode as Sortable

        val otherTreeNode = treeNodeCollection.getNode(position)
        val otherSortable = otherTreeNode.modelNode as? Sortable ?: return false

        if (!otherSortable.sortable) return false

        return thisSortable.canDropOn(otherSortable)
    }

    private fun onChildDrawHelper(
            viewHolder: RecyclerView.ViewHolder,
            dX: Float,
            dY: Float,
            isCurrentlyActive: Boolean,
    ) {
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

    class MyCallback : SimpleCallback(UP or DOWN, 0) {

        lateinit var dragHelper: DragHelper

        override fun isLongPressDragEnabled() = false

        override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder,
        ) = dragHelper.onMoveHelper(viewHolder, target)

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit

        override fun isItemViewSwipeEnabled() = false

        override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
            dragHelper.clearViewHelper(viewHolder)

            super.clearView(recyclerView, viewHolder)
        }

        override fun canDropOver(
                recyclerView: RecyclerView,
                current: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder,
        ) = dragHelper.canDropOverHelper(target)

        override fun onChildDraw(
                canvas: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean,
        ) = dragHelper.onChildDrawHelper(viewHolder, dX, dY, isCurrentlyActive)
    }
}