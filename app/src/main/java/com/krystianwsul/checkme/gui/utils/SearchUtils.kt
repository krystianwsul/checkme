package com.krystianwsul.checkme.gui.utils

import android.view.View
import androidx.annotation.CheckResult
import androidx.recyclerview.widget.RecyclerView
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.databinding.EmptyTextBinding
import com.krystianwsul.checkme.utils.animateVisibility
import com.krystianwsul.treeadapter.FilterCriteria
import com.krystianwsul.treeadapter.TreeViewAdapter
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.kotlin.Observables

@CheckResult
fun observeEmptySearchState(
        initializedObservable: Observable<Unit>,
        filterCriteriaObservable: Observable<FilterCriteria>,
        treeViewAdapter: () -> TreeViewAdapter<*>,
        search: (FilterCriteria, TreeViewAdapter.Placeholder?) -> Unit,
        recyclerView: RecyclerView,
        progressView: View,
        emptyTextBinding: EmptyTextBinding,
        immediate: () -> Boolean,
        emptyTextId: () -> Int?,
) = Observables.combineLatest(
        initializedObservable.map { true }.startWithItem(false),
        filterCriteriaObservable,
).subscribe { (isInitialized, searchData) ->
    if (isInitialized) {
        val emptyBefore = treeViewAdapter().displayedNodes.isEmpty()
        treeViewAdapter().updateDisplayedNodes { search(searchData, it) }

        val emptyAfter = treeViewAdapter().displayedNodes.isEmpty()
        if (emptyBefore && !emptyAfter)
            recyclerView.scrollToPosition(0)

        val hide = mutableListOf(progressView)
        val show = mutableListOf<View>()

        if (emptyAfter) {
            hide += recyclerView

            val (textId, drawableId) = if (searchData.hasSearch)
                R.string.noResults to R.drawable.search
            else
                emptyTextId() to R.drawable.empty

            if (textId != null) {
                show += emptyTextBinding.emptyTextLayout

                emptyTextBinding.emptyText.setText(textId)
                emptyTextBinding.emptyImage.setImageResource(drawableId)
            } else {
                hide += emptyTextBinding.emptyTextLayout
            }
        } else {
            show += recyclerView
            hide += emptyTextBinding.emptyTextLayout
        }

        animateVisibility(show, hide, immediate = immediate())
    } else {
        search(searchData, null)
    }
}!!