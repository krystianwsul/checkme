package com.krystianwsul.treeadapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import androidx.recyclerview.widget.RecyclerView
import com.jakewharton.rxrelay2.PublishRelay

class TreeViewAdapter<T : RecyclerView.ViewHolder>(
        val treeModelAdapter: TreeModelAdapter<T>,
        @param:LayoutRes @field:LayoutRes private val paddingLayout: Int?) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

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

    fun setTreeNodeCollection(treeNodeCollection: TreeNodeCollection<T>) {
        this.treeNodeCollection = treeNodeCollection
    }

    override fun getItemCount(): Int {
        if (treeNodeCollection == null)
            throw SetTreeNodeCollectionNotCalledException()

        return treeNodeCollection!!.displayedSize + if (paddingLayout != null) 1 else 0
    }

    fun hasActionMode() = treeModelAdapter.hasActionMode

    fun incrementSelected(x: Placeholder) = treeModelAdapter.incrementSelected(x)

    fun decrementSelected(x: Placeholder) = treeModelAdapter.decrementSelected(x)

    fun updateDisplayedNodes(action: () -> Unit) {
        if (treeNodeCollection == null)
            throw SetTreeNodeCollectionNotCalledException()

        check(!updating)

        val oldStates = treeNodeCollection!!.displayedNodes.map { it.state }
        val showPadding = paddingLayout != null

        updating = true
        action()
        updating = false

        val newStates = treeNodeCollection!!.displayedNodes.map { it.state }

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
                paddingComparison(oldItemPosition, newItemPosition)?.let { return it }

                return oldStates[oldItemPosition] == newStates[newItemPosition]
            }
        }).dispatchUpdatesTo(object : ListUpdateCallback {

            override fun onInserted(position: Int, count: Int) {
                notifyItemRangeInserted(position, count)

                if (position == 0)
                    treeModelAdapter.scrollToTop()
            }

            override fun onRemoved(position: Int, count: Int) = notifyItemRangeRemoved(position, count)

            override fun onMoved(fromPosition: Int, toPosition: Int) {
                notifyItemMoved(fromPosition, toPosition)

                if (toPosition == 0)
                    treeModelAdapter.scrollToTop()
            }

            override fun onChanged(position: Int, count: Int, payload: Any?) = notifyItemRangeChanged(position, count, payload)
        })

        updates.accept(Unit)
    }

    fun unselect(x: Placeholder) {
        if (treeNodeCollection == null)
            throw SetTreeNodeCollectionNotCalledException()

        treeNodeCollection!!.unselect(x)
    }

    fun selectAll(x: Placeholder) {
        if (treeNodeCollection == null)
            throw SetTreeNodeCollectionNotCalledException()

        treeNodeCollection!!.selectAll(x)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_PADDING) {
            checkNotNull(paddingLayout)

            PaddingHolder(LayoutInflater.from(parent.context).inflate(paddingLayout, parent, false)!!)
        } else {
            treeModelAdapter.onCreateViewHolder(parent, viewType)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        check(position >= 0)
        check(position < itemCount)

        if (treeNodeCollection == null)
            throw SetTreeNodeCollectionNotCalledException()

        val displayedSize = treeNodeCollection!!.displayedSize

        if (position < displayedSize) {
            val treeNode = treeNodeCollection!!.getNode(position)
            treeNode.onBindViewHolder(holder)
        } else {
            check(position == displayedSize)
            check(paddingLayout != null)
            check(position == itemCount - 1)
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

        return if (paddingLayout != null && position == treeNodeCollection!!.displayedSize)
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

    fun getTreeNodeCollection() = treeNodeCollection
            ?: throw SetTreeNodeCollectionNotCalledException()

    class SetTreeNodeCollectionNotCalledException : InitializationException("TreeViewAdapter.setTreeNodeCollection() has not been called.")

    private class PaddingHolder(view: View) : RecyclerView.ViewHolder(view)

    object Placeholder
}
