package com.krystianwsul.checkme.gui

import android.os.Handler
import android.util.DisplayMetrics
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import com.krystianwsul.checkme.gui.tasks.CreateTaskActivity
import com.krystianwsul.checkme.utils.TaskKey

interface ListItemAddedScroller {

    var scrollToTaskKey: TaskKey?

    fun findItem(): Int?

    val recyclerView: RecyclerView

    val listItemAddedListener: ListItemAddedListener

    fun tryScroll() {
        if (scrollToTaskKey == null)
            return

        val target = findItem() ?: return

        val linearLayoutManager = recyclerView.layoutManager as LinearLayoutManager

        if (linearLayoutManager.findFirstCompletelyVisibleItemPosition() == -1)
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

        if (target <= linearLayoutManager.findFirstCompletelyVisibleItemPosition()) {
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

        scrollToTaskKey = null

        return
    }

    fun checkCreatedTaskKey() {
        scrollToTaskKey = CreateTaskActivity.createdTaskKey
        CreateTaskActivity.createdTaskKey = null

        Handler().postDelayed({ tryScroll() }, 500)
    }

    private fun View.getAbsoluteTop(): Int {
        val arr = IntArray(2)
        getLocationOnScreen(arr)
        return arr[1]
    }
}