package com.krystianwsul.checkme.gui.tree

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jakewharton.rxbinding4.recyclerview.scrollStateChanges
import com.krystianwsul.treeadapter.TreeViewAdapter
import io.reactivex.rxjava3.core.Observable

fun getProgressShownObservable(
        recyclerView: RecyclerView,
        treeViewAdapterGetter: () -> TreeViewAdapter<*>,
): Observable<Unit> {
    return recyclerView.scrollStateChanges()
            .filter { it == RecyclerView.SCROLL_STATE_IDLE }
            .filter {
                val treeViewAdapter = treeViewAdapterGetter()

                val progressPosition = treeViewAdapter.itemCount - 1
                val lastVisiblePosition = (recyclerView.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()

                treeViewAdapter.showProgress && (progressPosition == lastVisiblePosition)
            }
            .map { }!!
}