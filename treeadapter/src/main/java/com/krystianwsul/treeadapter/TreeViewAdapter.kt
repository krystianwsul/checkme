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
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    internal var filterCriteria: Any? = null
        private set

    companion object {

        private const val TYPE_PADDING = 1000
    }

    private var treeNodeCollection: TreeNodeCollection<T>? = null

    val displayedNodes
        get() = treeNodeCollection?.displayedNodes
                ?: throw SetTreeNodeCollectionNotCalledException()

    val selectedNodes
        get() = treeNodeCollection?.selectedNodes ?: throw SetTreeNodeCollectionNotCalledException()

    private var updating = false

    val updates = PublishRelay.create<Unit>()

    val progressShown = PublishRelay.create<Unit>()

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

    override fun getItemCount(): Int {
        if (treeNodeCollection == null)
            throw SetTreeNodeCollectionNotCalledException()

        return treeNodeCollection!!.displayedSize + if (padding != null) 1 else 0
    }

    fun hasActionMode() = treeModelAdapter.hasActionMode

    fun incrementSelected(x: Placeholder) = treeModelAdapter.incrementSelected(x)

    fun decrementSelected(x: Placeholder) = treeModelAdapter.decrementSelected(x)

    fun updateDisplayedNodes(action: (Placeholder) -> Unit) {
        if (treeNodeCollection == null)
            throw SetTreeNodeCollectionNotCalledException()

        check(!updating)

        val oldStates = treeNodeCollection!!.displayedNodes.map { it.state }
        val oldShowProgress = showProgress

        val showPadding = padding != null

        updating = true
        action(Placeholder.instance)
        updating = false

        val newStates = treeNodeCollection!!.displayedNodes.map { it.state }
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

                return oldStates[oldItemPosition].modelState.same(newStates[newItemPosition].modelState)
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

    fun unselect(x: Placeholder) {
        if (treeNodeCollection == null) throw SetTreeNodeCollectionNotCalledException()

        treeNodeCollection!!.unselect(x)
    }

    fun selectAll() {
        if (treeNodeCollection == null) throw SetTreeNodeCollectionNotCalledException()

        updateDisplayedNodes(treeNodeCollection!!::selectAll)
    }

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
        if (treeNodeCollection == null)
            throw SetTreeNodeCollectionNotCalledException()

        val displayedSize = treeNodeCollection!!.displayedSize

        if (position < displayedSize) {
            val treeNode = treeNodeCollection!!.getNode(position)
            treeNode.onBindViewHolder(holder)
        } else {
            check(position == displayedSize)
            checkNotNull(padding)
            check(position == itemCount - 1)

            (holder as PaddingHolder).showProgress(showProgress)

            progressShown.accept(Unit)
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

        return if (padding != null && position == treeNodeCollection!!.displayedSize)
            TYPE_PADDING
        else
            treeNodeCollection!!.getItemViewType(position)
    }

    fun moveItem(from: Int, to: Int, x: Placeholder) {
        if (treeNodeCollection == null)
            throw SetTreeNodeCollectionNotCalledException()

        treeNodeCollection!!.moveItem(from, to, x)
    }

    fun setNewItemPosition(position: Int) {
        if (treeNodeCollection == null)
            throw SetTreeNodeCollectionNotCalledException()

        treeNodeCollection!!.setNewItemPosition(position)
    }

    private var updatingAfterNormalizationDisposable: Disposable? = null

    // todo search clean up setting null filterCriteria if no filtering is happening
    fun setFilterCriteria(filterCriteria: Any?, @Suppress("UNUSED_PARAMETER") placeholder: Placeholder) {
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
}
