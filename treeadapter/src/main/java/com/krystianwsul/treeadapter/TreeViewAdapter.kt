package com.krystianwsul.treeadapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import androidx.recyclerview.widget.RecyclerView
import com.jakewharton.rxrelay2.BehaviorRelay
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.schedulers.Schedulers

class TreeViewAdapter<T : RecyclerView.ViewHolder>(
        val treeModelAdapter: TreeModelAdapter<T>,
        private val padding: Pair<Int, Int>?,
        private val compositeDisposable: CompositeDisposable
) : RecyclerView.Adapter<RecyclerView.ViewHolder>(), ActionModeCallback by treeModelAdapter {

    internal var filterCriteria: FilterCriteria? = null
        private set

    companion object {

        private const val TYPE_PADDING = 1000
    }

    private var treeNodeCollection: TreeNodeCollection<T>? = null

    val displayedNodes
        get() = treeNodeCollection?.displayedNodes ?: throw SetTreeNodeCollectionNotCalledException()

    val selectedNodes
        get() = treeNodeCollection?.selectedNodes ?: throw SetTreeNodeCollectionNotCalledException()

    private var updating = false

    val updates = PublishRelay.create<Unit>()

    var showProgress = false
        set(value) {
            if (value)
                checkNotNull(padding)

            field = value
        }

    private val normalizeRelay = BehaviorRelay.create<Unit>()

    private val normalizedObservable = normalizeRelay.switchMap {
        treeNodeCollection!!.nodesObservable
                .firstOrError()
                .flatMapObservable {
                    Observable.fromCallable { treeNodeCollection!!.normalize() }
                            .subscribeOn(Schedulers.computation())
                            .map { true }
                            .startWith(false)
                            .observeOn(AndroidSchedulers.mainThread())
                }
    }
            .startWith(false)
            .replay(1)
            .apply { compositeDisposable += connect() }

    fun setTreeNodeCollection(treeNodeCollection: TreeNodeCollection<T>) {
        this.treeNodeCollection = treeNodeCollection

        normalizeRelay.accept(Unit)
    }

    override fun getItemCount() = displayedNodes.size + if (padding != null) 1 else 0

    fun updateDisplayedNodes(action: (Placeholder) -> Unit) {
        check(!updating)

        val oldStates = displayedNodes.map { it.state }
        val oldShowProgress = showProgress
        val oldFilterCriteria = filterCriteria

        val showPadding = padding != null

        updating = true

        action(Placeholder.instance)

        val newFilterCriteria = filterCriteria

        if (newFilterCriteria?.let { it != oldFilterCriteria } == true)
            updateSearchExpansion(newFilterCriteria, Placeholder.instance)

        updating = false

        val newStates = displayedNodes.map { it.state }
        val newShowProgress = showProgress

        DiffUtil.calculateDiff(object : DiffUtil.Callback() {

            private fun paddingComparison(oldItemPosition: Int, newItemPosition: Int): Boolean? {
                val oldIsPadding = showPadding && oldItemPosition == oldStates.size
                val newIsPadding = showPadding && newItemPosition == newStates.size

                if (oldIsPadding && newIsPadding)
                    return true

                if (oldIsPadding != newIsPadding)
                    return false

                return null
            }

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                paddingComparison(oldItemPosition, newItemPosition)?.let { return it }

                return oldStates[oldItemPosition].modelState.id == newStates[newItemPosition].modelState.id
            }

            override fun getOldListSize() = oldStates.size + (if (showPadding) 1 else 0)

            override fun getNewListSize() = newStates.size + (if (showPadding) 1 else 0)

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                paddingComparison(oldItemPosition, newItemPosition)?.let {
                    return if (it)
                        oldShowProgress == newShowProgress
                    else
                        it
                }

                return oldStates[oldItemPosition] == newStates[newItemPosition]
            }
        }).dispatchUpdatesTo(object : ListUpdateCallback {

            override fun onInserted(position: Int, count: Int) {
                notifyItemRangeInserted(position, count)

                if (position == 0)
                    treeModelAdapter.scrollToTop()
            }

            override fun onRemoved(position: Int, count: Int) {
                notifyItemRangeRemoved(position, count)
            }

            override fun onMoved(fromPosition: Int, toPosition: Int) {
                notifyItemMoved(fromPosition, toPosition)

                if (toPosition == 0)
                    treeModelAdapter.scrollToTop()
            }

            override fun onChanged(position: Int, count: Int, payload: Any?) {
                notifyItemRangeChanged(position, count, payload)
            }
        })

        updates.accept(Unit)
    }

    fun updateSearchExpansion(filterCriteria: FilterCriteria, placeholder: Placeholder) {
        treeNodeCollection!!.apply {
            collapseAll()

            if (filterCriteria.hasQuery) expandMatching(filterCriteria, placeholder)
        }
    }

    fun unselect(placeholder: Placeholder) = treeNodeCollection?.unselect(placeholder)
            ?: throw SetTreeNodeCollectionNotCalledException()

    fun selectAll() = treeNodeCollection?.let { updateDisplayedNodes(it::selectAll) }
            ?: throw SetTreeNodeCollectionNotCalledException()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_PADDING) {
            checkNotNull(padding)

            PaddingHolder(
                    LayoutInflater.from(parent.context).inflate(padding.first, parent, false)!!,
                    padding.second
            )
        } else {
            treeModelAdapter.onCreateViewHolder(parent, viewType)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val displayedSize = displayedNodes.size

        if (position < displayedSize) {
            treeNodeCollection!!.getNode(position).onBindViewHolder(holder)
        } else {
            check(position == displayedSize)
            checkNotNull(padding)
            check(position == itemCount - 1)

            (holder as PaddingHolder).showProgress(showProgress)
        }
    }

    override fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {
        if (holder !is PaddingHolder) // can't do conditional cast because of erasure
            @Suppress("UNCHECKED_CAST")
            treeModelAdapter.onViewAttachedToWindow(holder as T)
    }

    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        if (holder !is PaddingHolder) // can't do conditional cast because of erasure
            @Suppress("UNCHECKED_CAST")
            treeModelAdapter.onViewDetachedFromWindow(holder as T)
    }

    override fun getItemViewType(position: Int): Int {
        if (treeNodeCollection == null)
            throw SetTreeNodeCollectionNotCalledException()

        return if (padding != null && position == displayedNodes.size)
            TYPE_PADDING
        else
            treeNodeCollection!!.getItemViewType(position)
    }

    fun moveItem(
            from: Int,
            to: Int,
            placeholder: Placeholder
    ) = treeNodeCollection?.moveItem(from, to, placeholder)
            ?: throw SetTreeNodeCollectionNotCalledException()

    fun setNewItemPosition(position: Int) = treeNodeCollection?.setNewItemPosition(position)
            ?: throw SetTreeNodeCollectionNotCalledException()

    fun selectNode(position: Int) = treeNodeCollection?.selectNode(position)
            ?: throw SetTreeNodeCollectionNotCalledException()

    private var updatingAfterNormalizationDisposable: Disposable? = null

    fun setFilterCriteria(filterCriteria: FilterCriteria?, @Suppress("UNUSED_PARAMETER") placeholder: Placeholder) {
        updatingAfterNormalizationDisposable?.dispose()

        if (normalizedObservable.getCurrentValue()) {
            this.filterCriteria = filterCriteria
        } else {
            updatingAfterNormalizationDisposable = normalizedObservable.filter { it }
                    .subscribe {
                        updateDisplayedNodes { this.filterCriteria = filterCriteria }
                    }
                    .addTo(compositeDisposable)
        }
    }

    fun getTreeNodeCollection() = treeNodeCollection
            ?: throw SetTreeNodeCollectionNotCalledException()

    class SetTreeNodeCollectionNotCalledException : InitializationException("TreeViewAdapter.setTreeNodeCollection() has not been called.")

    private class PaddingHolder(view: View, private val progressId: Int) : RecyclerView.ViewHolder(view) {

        fun showProgress(showProgress: Boolean) {
            itemView.findViewById<View>(progressId).visibility = if (showProgress) View.VISIBLE else View.GONE
        }
    }

    class Placeholder private constructor() {

        companion object {

            val instance = Placeholder()
        }
    }

    interface FilterCriteria {

        val hasQuery: Boolean
    }
}
