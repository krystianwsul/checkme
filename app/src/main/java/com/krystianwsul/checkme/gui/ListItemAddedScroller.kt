package com.krystianwsul.checkme.gui

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

        if (target <= linearLayoutManager.findFirstCompletelyVisibleItemPosition()) {
            listItemAddedListener.setToolbarExpanded(true)
            recyclerView.smoothScrollToPosition(target)
        } else {
            listItemAddedListener.setToolbarExpanded(false)
            recyclerView.smoothScrollToPosition(target + 1)
        }

        scrollToTaskKey = null

        return
    }

    fun checkCreatedTaskKey() {
        scrollToTaskKey = CreateTaskActivity.createdTaskKey
        CreateTaskActivity.createdTaskKey = null

        tryScroll()
    }
}