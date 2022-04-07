package com.krystianwsul.checkme.gui.utils

import android.view.View
import com.jakewharton.rxrelay3.BehaviorRelay
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.databinding.EmptyTextBinding
import com.krystianwsul.checkme.gui.tree.AbstractHolder
import com.krystianwsul.checkme.gui.tree.BaseAdapter
import com.krystianwsul.checkme.utils.animateVisibility
import com.krystianwsul.common.criteria.SearchCriteria
import com.krystianwsul.treeadapter.TreeRecyclerView
import com.krystianwsul.treeadapter.TreeViewAdapter
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo

abstract class SearchDataManager<DATA : Any, MODEL_ADAPTER : BaseAdapter>(
    private val screenReadyObservable: Observable<Boolean>,
    private val dataObservable: Observable<DATA>,
) {

    protected abstract val recyclerView: TreeRecyclerView
    protected abstract val progressView: View
    protected abstract val emptyTextBinding: EmptyTextBinding

    protected abstract val emptyTextResId: Int?

    protected abstract val compositeDisposable: CompositeDisposable

    private var searchCriteria: SearchCriteria = SearchCriteria.empty

    private val treeViewAdapterRelay = BehaviorRelay.create<TreeViewAdapter<AbstractHolder>>()
    val treeViewAdapterInitialized get() = treeViewAdapterRelay.hasValue()
    val treeViewAdapterNullable get() = treeViewAdapterRelay.value
    val treeViewAdapter get() = treeViewAdapterNullable!!
    val treeViewAdapterSingle = treeViewAdapterRelay.firstOrError()

    var modelAdapter: MODEL_ADAPTER? = null
        private set

    protected abstract fun dataIsImmediate(data: DATA): Boolean

    protected abstract fun getSearchCriteriaFromData(data: DATA): SearchCriteria

    protected abstract fun instantiateAdapters(): Pair<MODEL_ADAPTER, TreeViewAdapter<AbstractHolder>>

    protected abstract fun attachTreeViewAdapter(treeViewAdapter: TreeViewAdapter<AbstractHolder>)

    protected abstract fun initializeModelAdapter(modelAdapter: MODEL_ADAPTER, data: DATA)

    protected abstract fun updateTreeViewAdapterAfterModelAdapterInitialization(
        treeViewAdapter: TreeViewAdapter<AbstractHolder>,
        data: DATA,
        initial: Boolean,
        placeholder: TreeViewAdapter.Placeholder,
    )

    protected abstract fun onDataChanged()

    private var data: DATA? = null

    fun subscribe() = observeData()

    private fun observeData() {
        screenReadyObservable.switchMap { if (it) dataObservable else Observable.never() }
            .subscribe { data ->
                val first = this.data == null
                val immediate = dataIsImmediate(data)

                val oldSearchCriteria = this.data?.let(::getSearchCriteriaFromData)

                val wasAtTop = recyclerView.layoutManager.findFirstCompletelyVisibleItemPosition() == 0

                this.data = data
                searchCriteria = getSearchCriteriaFromData(data)

                val emptyBefore: Boolean
                if (first) {
                    emptyBefore = true

                    val (modelAdapter, treeViewAdapter) = instantiateAdapters()

                    initializeModelAdapter(modelAdapter, data)

                    attachTreeViewAdapter(treeViewAdapter)

                    this.modelAdapter = modelAdapter
                    treeViewAdapterRelay.accept(treeViewAdapter)

                    treeViewAdapter.updateDisplayedNodes {
                        updateTreeViewAdapterAfterModelAdapterInitialization(treeViewAdapter, data, first, it)
                    }
                } else {
                    emptyBefore = isAdapterEmpty()

                    treeViewAdapter.updateDisplayedNodes {
                        initializeModelAdapter(modelAdapter!!, data)

                        updateTreeViewAdapterAfterModelAdapterInitialization(treeViewAdapter, data, first, it)
                    }
                }

                updateEmptyState(emptyBefore, immediate)

                onDataChanged()

                // this scrolls to top on search changes
                tryScrollToTopOnSearchChange(oldSearchCriteria, searchCriteria, wasAtTop)
            }
            .addTo(compositeDisposable)
    }

    private fun tryScrollToTopOnSearchChange(
        oldSearchCriteria: SearchCriteria?,
        newSearchCriteria: SearchCriteria,
        wasAtTop: Boolean,
    ) {
        if (!wasAtTop) {
            val oldSearch = oldSearchCriteria?.search
            val newSearch = newSearchCriteria.search

            if (listOfNotNull(oldSearch, newSearch).all { it.isEmpty }) return
            if (oldSearch == newSearch) return
        }

        recyclerView.scrollToPosition(0)
    }

    private fun isAdapterEmpty() = treeViewAdapter.displayedNodes.isEmpty()

    private fun updateEmptyState(emptyBefore: Boolean, immediate: Boolean) {
        val emptyAfter = isAdapterEmpty()

        if (emptyBefore && !emptyAfter) recyclerView.scrollToPosition(0)

        val hide = mutableListOf(progressView)
        val show = mutableListOf<View>()

        if (emptyAfter) {
            hide += recyclerView

            val (textId, drawableId) = if (searchCriteria.search.isEmpty)
                emptyTextResId to R.drawable.empty
            else
                R.string.noResults to R.drawable.search

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

        animateVisibility(show, hide, immediate = immediate)
    }
}