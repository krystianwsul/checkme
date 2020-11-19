package com.krystianwsul.checkme.gui.utils

import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.CheckResult
import androidx.recyclerview.widget.RecyclerView
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.utils.animateVisibility
import com.krystianwsul.checkme.viewmodels.NullableWrapper
import com.krystianwsul.treeadapter.TreeViewAdapter
import io.reactivex.Observable
import io.reactivex.rxkotlin.Observables

@CheckResult
fun <T : Any> observeEmptySearchState(
        initialized: Observable<Unit>,
        filterCriteria: Observable<NullableWrapper<T>>,
        treeViewAdapter: () -> TreeViewAdapter<*>,
        search: (T?, TreeViewAdapter.Placeholder) -> Unit,
        recyclerView: RecyclerView,
        progressView: View,
        emptyTextLayout: LinearLayout,
        immediate: () -> Boolean,
        emptyTextId: () -> Int?
) = Observables.combineLatest(initialized, filterCriteria).subscribe { (_, filterCriteriaWrapper) ->
    val emptyBefore = treeViewAdapter().displayedNodes.isEmpty()
    treeViewAdapter().updateDisplayedNodes { search(filterCriteriaWrapper.value, it) }

    val emptyAfter = treeViewAdapter().displayedNodes.isEmpty()
    if (emptyBefore && !emptyAfter)
        recyclerView.scrollToPosition(0)

    val hide = mutableListOf(progressView)
    val show = mutableListOf<View>()

    if (emptyAfter) {
        hide += recyclerView

        val (textId, drawableId) = filterCriteriaWrapper.value
                ?.let { R.string.noResults to R.drawable.search }
                ?: Pair(emptyTextId(), R.drawable.empty)

        if (textId != null) {
            show += emptyTextLayout

            emptyTextLayout.findViewById<TextView>(R.id.emptyText).setText(textId)
            emptyTextLayout.findViewById<ImageView>(R.id.emptyImage).setImageResource(drawableId)
        } else {
            hide += emptyTextLayout
        }
    } else {
        show += recyclerView
        hide += emptyTextLayout
    }

    animateVisibility(show, hide, immediate = immediate())
}!!