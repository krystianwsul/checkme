package com.krystianwsul.treeadapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import androidx.recyclerview.widget.RecyclerView
import com.jakewharton.rxrelay3.BehaviorRelay
import com.jakewharton.rxrelay3.PublishRelay
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.schedulers.Schedulers

class TreeViewAdapter<T : TreeHolder>(
        val treeModelAdapter: TreeModelAdapter<T>,
        private val paddingData: PaddingData,
        initialFilterCriteria: FilterCriteria = FilterCriteria.None,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>(), ActionModeCallback by treeModelAdapter {

    companion object {

        private const val TYPE_PADDING = 1000
    }

    internal var filterCriteria = initialFilterCriteria
        private set

    private var treeNodeCollection: TreeNodeCollection<T>? = null

    val displayedNodes
        get() = treeNodeCollection?.displayedNodes ?: throw SetTreeNodeCollectionNotCalledException()

    val selectedNodes
        get() = treeNodeCollection?.selectedNodes ?: throw SetTreeNodeCollectionNotCalledException()

    private var updating = false

    val updates = PublishRelay.create<Unit>()!!

    var showProgress = false

    private val showPadding get() = !paddingData.hideWithoutProgress || showProgress

    private val normalizeRelay = BehaviorRelay.create<Unit>()

    private val recyclerAttachedToWindowDisposable = CompositeDisposable()

    private val normalizedObservable = BehaviorRelay.createDefault(false)

    fun setTreeNodeCollection(treeNodeCollection: TreeNodeCollection<T>) {
        this.treeNodeCollection = treeNodeCollection

        normalizeRelay.accept(Unit)
    }

    override fun getItemCount() = displayedNodes.size + if (showPadding) 1 else 0

    fun updateDisplayedNodes(action: (Placeholder) -> Unit) {
        check(!updating)

        val oldStates = displayedNodes.map { it.state }
        val oldShowProgress = showProgress
        val oldFilterCriteria = filterCriteria
        val oldShowPadding = showPadding

        updating = true

        action(Placeholder.instance)

        val newFilterCriteria = filterCriteria

        if (
                (newFilterCriteria != oldFilterCriteria) &&
                (newFilterCriteria.expandMatches || oldFilterCriteria.expandMatches)
        ) {
            treeNodeCollection!!.apply {
                collapseAll()

                if (filterCriteria.expandMatches) expandMatching(filterCriteria.query)
            }
        }

        updating = false

        val newStates = displayedNodes.map { it.state }
        val newShowProgress = showProgress
        val newShowPadding = showPadding

        DiffUtil.calculateDiff(object : DiffUtil.Callback() {

            private fun paddingComparison(oldItemPosition: Int, newItemPosition: Int): Boolean? {
                val oldIsPadding = oldShowPadding && oldItemPosition == oldStates.size
                val newIsPadding = newShowPadding && newItemPosition == newStates.size

                if (oldIsPadding && newIsPadding) return true
                if (oldIsPadding != newIsPadding) return false

                return null
            }

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                paddingComparison(oldItemPosition, newItemPosition)?.let { return it }

                return oldStates[oldItemPosition].modelState.id == newStates[newItemPosition].modelState.id
            }

            override fun getOldListSize() = oldStates.size + (if (oldShowPadding) 1 else 0)

            override fun getNewListSize() = newStates.size + (if (newShowPadding) 1 else 0)

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

                if (position == 0) treeModelAdapter.scrollToTop()
            }

            override fun onRemoved(position: Int, count: Int) {
                notifyItemRangeRemoved(position, count)
            }

            override fun onMoved(fromPosition: Int, toPosition: Int) {
                notifyItemMoved(fromPosition, toPosition)

                if (toPosition == 0) treeModelAdapter.scrollToTop()
            }

            override fun onChanged(position: Int, count: Int, payload: Any?) {
                notifyItemRangeChanged(position, count, payload)
            }
        })

        updates.accept(Unit)
    }

    fun unselect(placeholder: Placeholder) = treeNodeCollection?.unselect(placeholder)
            ?: throw SetTreeNodeCollectionNotCalledException()

    fun selectAll() = treeNodeCollection?.let { updateDisplayedNodes(it::selectAll) }
            ?: throw SetTreeNodeCollectionNotCalledException()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_PADDING) {
            check(showPadding)

            PaddingHolder(
                    LayoutInflater.from(parent.context).inflate(paddingData.layoutId, parent, false)!!,
                    paddingData.viewId
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
            check(showPadding)
            check(position == itemCount - 1)

            (holder as PaddingHolder).showProgress(showProgress)
        }
    }

    private val attachedHolders = mutableSetOf<TreeHolder>()
    private var recyclerAttached = false

    override fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {
        check(recyclerAttached)

        if (holder !is TreeHolder) return

        check(holder !in attachedHolders)

        attachedHolders += holder
        holder.startRx()
    }

    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        check(recyclerAttached)

        if (holder !is TreeHolder) return

        check(holder in attachedHolders)

        holder.stopRx()
        attachedHolders -= holder
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        check(recyclerView is TreeRecyclerView)
    }

    fun onRecyclerAttachedToWindow() {
        check(!recyclerAttached)

        recyclerAttached = true

        normalizeRelay.switchMap {
            treeNodeCollection!!.nodesObservable
                    .firstOrError()
                    .flatMapObservable {
                        Observable.fromCallable { treeNodeCollection!!.normalize() }
                                .subscribeOn(Schedulers.computation())
                                .map { true }
                                .startWithItem(false)
                                .observeOn(AndroidSchedulers.mainThread())
                    }
        }
                .subscribe(normalizedObservable::accept)
                .addTo(recyclerAttachedToWindowDisposable)

        attachedHolders.forEach { it.startRx() }
    }

    fun onRecyclerDetachedFromWindow() {
        check(recyclerAttached)

        attachedHolders.forEach { it.stopRx() }

        recyclerAttachedToWindowDisposable.clear()

        recyclerAttached = false
    }

    override fun getItemViewType(position: Int): Int {
        if (treeNodeCollection == null)
            throw SetTreeNodeCollectionNotCalledException()

        return if (showPadding && position == displayedNodes.size)
            TYPE_PADDING
        else
            treeNodeCollection!!.getItemViewType(position)
    }

    fun moveItem(
            from: Int,
            to: Int,
            placeholder: Placeholder,
    ) = treeNodeCollection?.moveItem(from, to, placeholder)
            ?: throw SetTreeNodeCollectionNotCalledException()

    fun setNewItemPosition(position: Int) = treeNodeCollection?.setNewItemPosition(position)
            ?: throw SetTreeNodeCollectionNotCalledException()

    fun selectNode(position: Int) = treeNodeCollection?.selectNode(position)
            ?: throw SetTreeNodeCollectionNotCalledException()

    private var updatingAfterNormalizationDisposable: Disposable? = null

    fun setFilterCriteria(filterCriteria: FilterCriteria, @Suppress("UNUSED_PARAMETER") placeholder: Placeholder) {
        updatingAfterNormalizationDisposable?.dispose()

        if (normalizedObservable.getCurrentValue() || filterCriteria.query.isEmpty()) {
            this.filterCriteria = filterCriteria
        } else {
            updatingAfterNormalizationDisposable = normalizedObservable.filter { it }
                    .subscribe { updateDisplayedNodes { this.filterCriteria = filterCriteria } }
                    .addTo(recyclerAttachedToWindowDisposable)
        }
    }

    fun getTreeNodeCollection() = treeNodeCollection
            ?: throw SetTreeNodeCollectionNotCalledException()

    class SetTreeNodeCollectionNotCalledException : InitializationException("TreeViewAdapter.setTreeNodeCollection() has not been called.")

    private class PaddingHolder(view: View, private val progressId: Int) : RecyclerView.ViewHolder(view) {

        fun showProgress(showProgress: Boolean) {
            itemView.findViewById<View>(progressId).visibility = if (showProgress) View.VISIBLE else View.INVISIBLE
        }
    }

    class Placeholder private constructor() {

        companion object {

            internal val instance = Placeholder()
        }
    }

    data class PaddingData(
            @LayoutRes val layoutId: Int,
            @IdRes val viewId: Int,
            val hideWithoutProgress: Boolean = false,
    )
}
