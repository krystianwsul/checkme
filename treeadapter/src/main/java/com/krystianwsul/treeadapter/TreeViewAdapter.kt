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
import com.krystianwsul.treeadapter.locker.AdapterLocker
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

    val displayableNodes get() = treeNodeCollection?.displayableNodes ?: throw SetTreeNodeCollectionNotCalledException()

    val selectedNodes
        get() = treeNodeCollection?.selectedNodes ?: throw SetTreeNodeCollectionNotCalledException()

    private var updating = false

    val updates = PublishRelay.create<Unit>()!!

    var showProgress = false

    private val showPadding get() = !paddingData.hideWithoutProgress || showProgress

    private val normalizeRelay = BehaviorRelay.create<Unit>()

    private val recyclerAttachedToWindowDisposable = CompositeDisposable()

    private val normalizedObservable = BehaviorRelay.createDefault(false)

    var locker: AdapterLocker<T>? = null
        private set

    fun setTreeNodeCollection(treeNodeCollection: TreeNodeCollection<T>) {
        if (this.treeNodeCollection == null) {
            check(!updating)
            check(locker == null)

            this.treeNodeCollection = treeNodeCollection

            treeNodeCollection.nodesObservable
                    .firstOrError()
                    .subscribe { _ ->
                        check(!updating)
                        check(locker == null)

                        locker = AdapterLocker()

                        filterCriteria.search?.let {
                            if (it.expandMatches) treeNodeCollection.expandMatching(it, false)
                        }
                    }
        } else {
            check(updating)
            check(locker == null)

            this.treeNodeCollection = treeNodeCollection
        }

        normalizeRelay.accept(Unit)
    }

    override fun getItemCount() = displayedNodes.size + if (showPadding) 1 else 0

    private fun getStates() = displayedNodes.map { it.state }

    private var ignoreNextScroll: Boolean = false

    fun ignoreNextScroll() {
        check(!ignoreNextScroll)

        ignoreNextScroll = true
    }

    fun updateDisplayedNodes(action: (Placeholder) -> Unit) = updateDisplayedNodes(false, action)

    fun updateDisplayedNodes(useMove: Boolean, action: (Placeholder) -> Unit) {
        check(!updating)
        checkNotNull(locker)

        val oldStates = getStates()
        val oldShowProgress = showProgress
        val oldFilterCriteria = filterCriteria
        val oldShowPadding = showPadding

        updating = true
        locker = null

        action(Placeholder.instance)

        val newFilterCriteria = filterCriteria
        locker = AdapterLocker()

        if (
                (newFilterCriteria != oldFilterCriteria) &&
                (newFilterCriteria.expandMatches || oldFilterCriteria.expandMatches)
        ) {
            treeNodeCollection!!.apply {
                resetExpansion(true, Placeholder.instance)

                val search = filterCriteria.search

                if (search?.expandMatches == true) {
                    val visibleCount = nodes.count { it.visible() }

                    expandMatching(search, visibleCount == 1)
                }
            }
        }

        updating = false

        val newStates = getStates()
        val newShowProgress = showProgress
        val newShowPadding = showPadding

        val (oldIds, newIds) =
            treeModelAdapter.mutateIds(oldStates.map { it.modelState.id }, newStates.map { it.modelState.id })

        check(oldIds.size == oldStates.size)
        check(newIds.size == newStates.size)

        DiffUtil.calculateDiff(object : DiffUtil.Callback() {

            private fun paddingComparison(oldItemPosition: Int, newItemPosition: Int): Boolean? {
                val oldIsPadding = oldShowPadding && oldItemPosition == oldStates.size
                val newIsPadding = newShowPadding && newItemPosition == newStates.size

                /**
                 * only treat them as equal if progress is shown.  Otherwise don't, to prevent weird scroll issue.
                 *
                 * Issue: padding is shown in basically any list.  So in a situation where all items change, and the
                 * dataset is taller than the screen, you'll get a scroll-to-top animation (since the padding was a
                 * fixed point between the two datasets.)
                 */
                if (oldIsPadding && newIsPadding) return (oldShowProgress || newShowProgress)
                if (oldIsPadding != newIsPadding) return false

                return null
            }

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                paddingComparison(oldItemPosition, newItemPosition)?.let { return it }

                return oldIds[oldItemPosition] == newIds[newItemPosition]
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

            override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
                paddingComparison(oldItemPosition, newItemPosition)?.let { return null }

                return oldStates[oldItemPosition].getPayload(newStates[newItemPosition])
            }
        }).dispatchUpdatesTo(object : ListUpdateCallback {

            private val ignoreScroll = this@TreeViewAdapter.ignoreNextScroll

            init {
                this@TreeViewAdapter.ignoreNextScroll = false
            }

            override fun onInserted(position: Int, count: Int) {
                notifyItemRangeInserted(position, count)

                checkScroll(position)
            }

            override fun onRemoved(position: Int, count: Int) = notifyItemRangeRemoved(position, count)

            override fun onMoved(fromPosition: Int, toPosition: Int) {
                if (useMove) {
                    notifyItemMoved(fromPosition, toPosition)
                } else {
                    notifyItemRemoved(fromPosition)
                    notifyItemInserted(toPosition)

                    checkScroll(toPosition)
                }
            }

            private fun checkScroll(position: Int) {
                if (position == 0 && !ignoreScroll) treeModelAdapter.scrollToTop()
            }

            override fun onChanged(position: Int, count: Int, payload: Any?) =
                notifyItemRangeChanged(position, count, payload)
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

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: List<Any>) {
        if (position == displayedNodes.size) {
            check(showPadding)
            check(payloads.isEmpty())

            super.onBindViewHolder(holder, position, payloads)
        } else {
            treeNodeCollection!!.getNode(position, PositionMode.DISPLAYED).apply {
                if (payloads.isNotEmpty()) {
                    check(payloads.all { it is TreeNode.PayloadSeparator })

                    onPayload(holder)
                } else {
                    super.onBindViewHolder(holder, position, payloads)
                }
            }
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

    fun moveItem(from: Int, to: Int, placeholder: Placeholder) =
            treeNodeCollection?.moveItem(from, to, placeholder) ?: throw SetTreeNodeCollectionNotCalledException()

    fun setNewItemPosition(position: Int) = treeNodeCollection?.setNewItemPosition(position)
            ?: throw SetTreeNodeCollectionNotCalledException()

    fun selectNode(position: Int) = treeNodeCollection?.selectNode(position)
            ?: throw SetTreeNodeCollectionNotCalledException()

    private var updatingAfterNormalizationDisposable: Disposable? = null

    fun setFilterCriteria(filterCriteria: FilterCriteria, @Suppress("UNUSED_PARAMETER") placeholder: Placeholder) {
        updatingAfterNormalizationDisposable?.dispose()

        if (normalizedObservable.getCurrentValue() || !filterCriteria.needsNormalization) {
            this.filterCriteria = filterCriteria
        } else {
            updatingAfterNormalizationDisposable = normalizedObservable.filter { it }
                .subscribe { updateDisplayedNodes { this.filterCriteria = filterCriteria } }
                .addTo(recyclerAttachedToWindowDisposable)
        }
    }

    fun getTreeNodeCollection() = treeNodeCollection
            ?: throw SetTreeNodeCollectionNotCalledException()

    fun clearExpansionStates() = updateDisplayedNodes {
        treeNodeCollection?.resetExpansion(false, it) ?: throw SetTreeNodeCollectionNotCalledException()
    }

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
