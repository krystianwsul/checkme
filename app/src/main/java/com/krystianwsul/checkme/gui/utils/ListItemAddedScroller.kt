package com.krystianwsul.checkme.gui.utils

import android.util.DisplayMetrics
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import com.krystianwsul.checkme.gui.base.ListItemAddedListener
import com.krystianwsul.checkme.gui.edit.EditActivity
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.common.utils.TaskKey
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import java.util.concurrent.TimeUnit

interface ListItemAddedScroller {

    fun findItem(): Int?

    val recyclerView: RecyclerView

    val listItemAddedListener: ListItemAddedListener

    val scrollDisposable: CompositeDisposable

    fun scrollToPosition(target: Int) {
        val linearLayoutManager = recyclerView.layoutManager as LinearLayoutManager
        val firstCompletelyVisibleItemPosition = linearLayoutManager.findFirstCompletelyVisibleItemPosition()

        if (firstCompletelyVisibleItemPosition == -1) return

        fun smoothScroll(position: Int) {
            val scroller = object : LinearSmoothScroller(recyclerView.context) {

                override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics) = 100f / displayMetrics.densityDpi
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

                if (targetBottom > anchorTop) scrollToBottom()
            }
        }
    }

    fun tryScroll() {
        delay {
            val target = findItem() ?: return@delay

            scrollToPosition(target)

            setScrollTargetMatcher(null)
        }
    }

    fun delay(action: () -> Unit) {
        Single.just(Unit)
                .delay(500, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy { action() }
                .addTo(scrollDisposable)
    }

    fun checkCreatedTaskKey() {
        EditActivity.createdTaskKey?.let { setScrollTargetMatcher(ScrollTargetMatcher.Task(it)) }
        EditActivity.createdTaskKey = null

        tryScroll()
    }

    private fun View.getAbsoluteTop() = IntArray(2).let {
        getLocationOnScreen(it)
        it[1]
    }

    fun scrollToTop() = delay { scrollToPosition(0) }

    fun setScrollTargetMatcher(scrollTargetMatcher: ScrollTargetMatcher.Task?)

    sealed interface ScrollTargetMatcher {

        class Task(val taskKey: TaskKey) : ScrollTargetMatcher

        class Instance(val instanceKey: InstanceKey) : ScrollTargetMatcher
    }
}