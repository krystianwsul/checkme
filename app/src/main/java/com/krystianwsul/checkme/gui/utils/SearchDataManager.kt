package com.krystianwsul.checkme.gui.utils

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.jakewharton.rxrelay3.BehaviorRelay
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.databinding.EmptyTextBinding
import com.krystianwsul.checkme.gui.tree.AbstractHolder
import com.krystianwsul.checkme.gui.tree.BaseAdapter
import com.krystianwsul.checkme.utils.animateVisibility
import com.krystianwsul.treeadapter.FilterCriteria
import com.krystianwsul.treeadapter.TreeViewAdapter
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.plusAssign

abstract class SearchDataManager<DATA : Any, MODEL_ADAPTER : BaseAdapter>(
        private val screenReadyObservable: Observable<Boolean>,
        private val dataObservable: Observable<DATA>,
) {

    protected abstract val recyclerView: RecyclerView
    protected abstract val progressView: View
    protected abstract val emptyTextBinding: EmptyTextBinding

    protected abstract val emptyTextResId: Int?

    protected abstract val compositeDisposable: CompositeDisposable

    protected abstract val filterCriteriaObservable: Observable<FilterCriteria>

    var filterCriteria: FilterCriteria = FilterCriteria.None
        private set

    private val treeViewAdapterRelay = BehaviorRelay.create<TreeViewAdapter<AbstractHolder>>()!!
    val treeViewAdapterInitialized get() = treeViewAdapterRelay.hasValue()
    val treeViewAdapterNullable get() = treeViewAdapterRelay.value
    val treeViewAdapter get() = treeViewAdapterNullable!!
    val treeViewAdapterSingle = treeViewAdapterRelay.firstOrError()!!

    var modelAdapter: MODEL_ADAPTER? = null
        private set

    protected abstract fun dataIsImmediate(data: DATA): Boolean

    protected abstract fun getFilterCriteriaFromData(data: DATA): FilterCriteria?

    protected abstract fun filterDataChangeRequiresReinitializingModelAdapter(
            oldFilterCriteria: FilterCriteria,
            newFilterCriteria: FilterCriteria,
    ): Boolean

    protected abstract fun instantiateAdapters(filterCriteria: FilterCriteria):
            Pair<MODEL_ADAPTER, TreeViewAdapter<AbstractHolder>>

    protected abstract fun attachTreeViewAdapter(treeViewAdapter: TreeViewAdapter<AbstractHolder>)

    protected abstract fun initializeModelAdapter(modelAdapter: MODEL_ADAPTER, data: DATA, filterCriteria: FilterCriteria)

    protected abstract fun updateTreeViewAdapterAfterModelAdapterInitialization(
            treeViewAdapter: TreeViewAdapter<AbstractHolder>,
            data: DATA,
            initial: Boolean,
            placeholder: TreeViewAdapter.Placeholder,
    )

    protected abstract fun onDataChanged()
    protected abstract fun onFilterCriteriaChanged()

    fun setInitialFilterCriteria(filterCriteria: FilterCriteria) {
        if (modelAdapter == null)
            this.filterCriteria = filterCriteria
        else
            check(this.filterCriteria == filterCriteria)
    }

    private var data: DATA? = null

    fun subscribe() {
        observeData()
        observeFilterCriteria()
    }

    private fun observeData() {
        screenReadyObservable.switchMap { if (it) dataObservable else Observable.never() }
                .subscribe { data ->
                    val first = this.data == null
                    val immediate = dataIsImmediate(data)

                    this.data = data
                    getFilterCriteriaFromData(data)?.let { filterCriteria = it }

                    val emptyBefore: Boolean
                    if (first) {
                        emptyBefore = true

                        val (modelAdapter, treeViewAdapter) = instantiateAdapters(filterCriteria)

                        initializeModelAdapter(modelAdapter, data, filterCriteria)

                        attachTreeViewAdapter(treeViewAdapter)

                        this.modelAdapter = modelAdapter
                        treeViewAdapterRelay.accept(treeViewAdapter)

                        treeViewAdapter.updateDisplayedNodes {
                            updateTreeViewAdapterAfterModelAdapterInitialization(treeViewAdapter, data, first, it)
                        }
                    } else {
                        emptyBefore = isAdapterEmpty()

                        treeViewAdapter.updateDisplayedNodes {
                            initializeModelAdapter(modelAdapter!!, data, filterCriteria)

                            updateTreeViewAdapterAfterModelAdapterInitialization(treeViewAdapter, data, first, it)

                            treeViewAdapter.setFilterCriteria(filterCriteria, it)
                        }
                    }

                    updateEmptyState(emptyBefore, immediate)

                    onDataChanged()
                }
                .addTo(compositeDisposable)
    }

    private fun observeFilterCriteria() {
        compositeDisposable += filterCriteriaObservable.subscribe { filterCriteria ->
            val oldFilterCriteria = this.filterCriteria
            this.filterCriteria = filterCriteria

            onFilterCriteriaChanged()

            if (treeViewAdapterInitialized) {
                val emptyBefore = isAdapterEmpty()

                if (filterDataChangeRequiresReinitializingModelAdapter(oldFilterCriteria, filterCriteria)) {
                    treeViewAdapter.updateDisplayedNodes {
                        initializeModelAdapter(modelAdapter!!, data!!, filterCriteria)

                        updateTreeViewAdapterAfterModelAdapterInitialization(treeViewAdapter, data!!, false, it)

                        treeViewAdapter.setFilterCriteria(filterCriteria, it)
                    }
                } else {
                    treeViewAdapter.updateDisplayedNodes { treeViewAdapter.setFilterCriteria(filterCriteria, it) }
                }

                updateEmptyState(emptyBefore, false)
            }
        }
    }

    private fun isAdapterEmpty() = treeViewAdapter.displayedNodes.isEmpty()

    private fun updateEmptyState(emptyBefore: Boolean, immediate: Boolean) {
        val emptyAfter = isAdapterEmpty()
        if (emptyBefore && !emptyAfter)
            recyclerView.scrollToPosition(0)

        val hide = mutableListOf(progressView)
        val show = mutableListOf<View>()

        if (emptyAfter) {
            hide += recyclerView

            val (textId, drawableId) = if (filterCriteria.query.isNotEmpty())
                R.string.noResults to R.drawable.search
            else
                emptyTextResId to R.drawable.empty

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