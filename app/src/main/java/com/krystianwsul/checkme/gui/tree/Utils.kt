package com.krystianwsul.checkme.gui.tree

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jakewharton.rxbinding4.recyclerview.scrollStateChanges
import com.jakewharton.rxbinding4.view.layoutChanges
import com.krystianwsul.treeadapter.TreeViewAdapter
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.merge

fun getProgressShownObservable(
    recyclerView: RecyclerView,
    treeViewAdapterSingle: Single<TreeViewAdapter<*>>,
): Observable<Unit> {
    return treeViewAdapterSingle.flatMapObservable { treeViewAdapter ->
        listOf(
            treeViewAdapter.listUpdates.switchMapSingle { recyclerView.layoutChanges().firstOrError() },
            recyclerView.scrollStateChanges()
                .filter { it == RecyclerView.SCROLL_STATE_IDLE }
                .map { },
        ).merge().filter {
            val progressPosition = treeViewAdapter.itemCount - 1
            val lastVisiblePosition = (recyclerView.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()

            treeViewAdapter.showProgress && (progressPosition == lastVisiblePosition)
        }
    }
}