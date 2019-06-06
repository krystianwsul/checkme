package com.krystianwsul.checkme.gui

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
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

        if (target <= linearLayoutManager.findFirstCompletelyVisibleItemPosition()) {
            listItemAddedListener.setToolbarExpanded(true)
            recyclerView.smoothScrollToPosition(target)
        } else {
            fun scrollToBottom() {
                listItemAddedListener.setToolbarExpanded(false)
                recyclerView.smoothScrollToPosition(target + 1)
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

        tryScroll()
    }

    private fun View.getAbsoluteTop(): Int {
        /*
        val rect = Rect()
        getGlobalVisibleRect(rect)

        return rect.top
        */

        val arr = IntArray(2)
        getLocationOnScreen(arr)
        return arr[1]
    }
}