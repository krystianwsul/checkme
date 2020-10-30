package com.krystianwsul.checkme.gui.utils

import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import com.krystianwsul.checkme.gui.base.ListItemAddedListener
import com.krystianwsul.checkme.gui.edit.EditActivity
import com.krystianwsul.common.utils.TaskKey

interface ListItemAddedScroller {

    var scrollToTaskKey: TaskKey?

    fun findItem(): Int?

    val recyclerView: RecyclerView

    val listItemAddedListener: ListItemAddedListener

    fun scrollToPosition(target: Int) {
        val linearLayoutManager = recyclerView.layoutManager as LinearLayoutManager
        val firstCompletelyVisibleItemPosition = linearLayoutManager.findFirstCompletelyVisibleItemPosition()

        if (firstCompletelyVisibleItemPosition == -1)
            return

        fun smoothScroll(position: Int) {
            val scroller = object : LinearSmoothScroller(recyclerView.context) {

                override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics): Float {
                    return 100f / displayMetrics.densityDpi
                }
            }

            scroller.targetPosition = position

            recyclerView.layoutManager!!.startSmoothScroll(scroller)
        }

        if (target <= firstCompletelyVisibleItemPosition) {
            listItemAddedListener.setToolbarExpanded(true)
            smoothScroll(target)
        } else {
            fun scrollToBottom() {
                listItemAddedListener.setToolbarExpanded(false)
                smoothScroll(target + 1)
            }

            if (target > linearLayoutManager.findLastCompletelyVisibleItemPosition()) {
                scrollToBottom()
            } else {
                val anchorTop = listItemAddedListener.anchor.getAbsoluteTop()

                val targetView = linearLayoutManager.findViewByPosition(target)!!

                val targetBottom = targetView.getAbsoluteTop() + targetView.height

                if (targetBottom > anchorTop)
                    scrollToBottom()
            }
        }
    }

    fun tryScroll() {
        delay {
            if (scrollToTaskKey == null)
                return@delay

            val target = findItem() ?: return@delay

            scrollToPosition(target)

            scrollToTaskKey = null
        }
    }

    fun delay(action: () -> Unit) = Handler(Looper.getMainLooper()).postDelayed(action, 500)

    fun checkCreatedTaskKey() {
        scrollToTaskKey = EditActivity.createdTaskKey
        EditActivity.createdTaskKey = null

        tryScroll()
    }

    private fun View.getAbsoluteTop(): Int {
        val arr = IntArray(2)
        getLocationOnScreen(arr)
        return arr[1]
    }

    fun scrollToTop() {
        delay { scrollToPosition(0) }
    }
}