package com.krystianwsul.treeadapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.*

class TreeViewAdapter(
        val treeModelAdapter: TreeModelAdapter,
        @param:LayoutRes @field:LayoutRes private val paddingLayout: Int? = null) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {

        private const val TYPE_PADDING = 1000
    }

    var showPadding = false

    private var treeNodeCollection: TreeNodeCollection? = null

    val selectedNodes: List<TreeNode>
        get() {
            if (treeNodeCollection == null)
                throw SetTreeNodeCollectionNotCalledException()

            return treeNodeCollection!!.selectedNodes
        }

    var query: String = ""

    private var updating = false

    fun setTreeNodeCollection(treeNodeCollection: TreeNodeCollection) {
        this.treeNodeCollection = treeNodeCollection
    }

    override fun getItemCount(): Int {
        if (treeNodeCollection == null)
            throw SetTreeNodeCollectionNotCalledException()

        return treeNodeCollection!!.displayedSize + if (showPadding) 1 else 0
    }

    fun hasActionMode() = treeModelAdapter.hasActionMode

    fun incrementSelected(x: TreeViewAdapter.Placeholder) = treeModelAdapter.incrementSelected(x)

    fun decrementSelected(x: TreeViewAdapter.Placeholder) = treeModelAdapter.decrementSelected(x)

    fun updateDisplayedNodes(action: () -> Unit) = updateDisplayedNodes(false, action)

    fun updateDisplayedNodes(forceChange: Boolean, action: () -> Unit) {
        if (treeNodeCollection == null)
            throw SetTreeNodeCollectionNotCalledException()

        check(!updating)

        val oldStates = treeNodeCollection!!.displayedNodes.map { it.state }

        updating = true
        action()
        updating = false

        val newStates = treeNodeCollection!!.displayedNodes.map { it.state }

        val target = if (forceChange) {
            val listUpdateCallback = object : ListUpdateCallback {

                val states = BooleanArray(oldStates.size) { false }.toMutableList()

                override fun onChanged(position: Int, count: Int, payload: Any?) {
                    for (i in 0 until count)
                        states[position + i] = true

                    notifyItemRangeChanged(position, count)
                }

                override fun onMoved(fromPosition: Int, toPosition: Int) {
                    states.removeAt(fromPosition)
                    states.add(toPosition, true)

                    notifyItemMoved(fromPosition, toPosition)
                }

                override fun onInserted(position: Int, count: Int) {
                    for (i in 0 until count)
                        states.add(position, true)

                    notifyItemRangeInserted(position, count)
                }

                override fun onRemoved(position: Int, count: Int) {
                    for (i in 0 until count)
                        states.removeAt(position)

                    notifyItemRangeRemoved(position, count)
                }

                fun done() {
                    BatchingListUpdateCallback(AdapterListUpdateCallback(this@TreeViewAdapter)).apply {
                        states.mapIndexed { index, value -> Pair(index, value) }
                                .filterNot { it.second }
                                .map { it.first }
                                .forEach {
                                    onChanged(it, 1, null)
                                }

                        dispatchLastEvent()
                    }
                }
            }

            object : BatchingListUpdateCallback(listUpdateCallback) {

                private var me = false

                override fun onChanged(position: Int, count: Int, payload: Any?) {
                    me = true
                    super.onChanged(position, count, payload)
                    me = false
                }

                override fun onInserted(position: Int, count: Int) {
                    me = true
                    super.onInserted(position, count)
                    me = false
                }

                override fun onMoved(fromPosition: Int, toPosition: Int) {
                    me = true
                    super.onMoved(fromPosition, toPosition)
                    me = false
                }

                override fun onRemoved(position: Int, count: Int) {
                    me = true
                    super.onRemoved(position, count)
                    me = false
                }

                override fun dispatchLastEvent() {
                    super.dispatchLastEvent()

                    if (!me)
                        listUpdateCallback.done()
                }
            }
        } else {
            AdapterListUpdateCallback(this@TreeViewAdapter)
        }

        DiffUtil.calculateDiff(object : DiffUtil.Callback() {

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                if (showPadding) {
                    if (oldItemPosition == oldStates.size && newItemPosition == newStates.size)
                        return true
                    else if (oldItemPosition == oldStates.size || newItemPosition == newStates.size)
                        return false
                }

                return oldStates[oldItemPosition].modelState.same(newStates[newItemPosition].modelState)
            }

            override fun getOldListSize() = oldStates.size + (if (showPadding) 1 else 0)

            override fun getNewListSize() = newStates.size + (if (showPadding) 1 else 0)

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                if (showPadding) {
                    if (oldItemPosition == oldStates.size && newItemPosition == newStates.size)
                        return true
                    else if (oldItemPosition == oldStates.size || newItemPosition == newStates.size)
                        return false
                }

                val oldState = oldStates[oldItemPosition]
                val newState = newStates[newItemPosition]

                if (oldState.isExpanded != newState.isExpanded)
                    return false

                if (oldState.isSelected != newState.isSelected)
                    return false

                if (oldState.separatorVisibility != newState.separatorVisibility)
                    return false

                if (oldState.expandVisible != newState.expandVisible)
                    return false

                return oldState.modelState == newState.modelState
            }
        }).dispatchUpdatesTo(target)
    }

    fun unselect(x: TreeViewAdapter.Placeholder) {
        if (treeNodeCollection == null)
            throw SetTreeNodeCollectionNotCalledException()

        treeNodeCollection!!.unselect(x)
    }

    fun selectAll(x: TreeViewAdapter.Placeholder) {
        if (treeNodeCollection == null)
            throw SetTreeNodeCollectionNotCalledException()

        check(!treeModelAdapter.hasActionMode)

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

        val itemCount = itemCount
        check(position < itemCount)

        if (treeNodeCollection == null)
            throw SetTreeNodeCollectionNotCalledException()

        val displayedSize = treeNodeCollection!!.displayedSize

        if (position < displayedSize) {
            val treeNode = treeNodeCollection!!.getNode(position)
            treeNode.onBindViewHolder(holder)
        } else {
            check(position == displayedSize)
            check(showPadding)
            check(position == itemCount - 1)
        }
    }

    override fun getItemViewType(position: Int): Int {
        if (treeNodeCollection == null)
            throw SetTreeNodeCollectionNotCalledException()

        return if (showPadding && position == treeNodeCollection!!.displayedSize)
            TYPE_PADDING
        else
            treeNodeCollection!!.getItemViewType(position)
    }

    fun moveItem(from: Int, to: Int, x: TreeViewAdapter.Placeholder) {
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
