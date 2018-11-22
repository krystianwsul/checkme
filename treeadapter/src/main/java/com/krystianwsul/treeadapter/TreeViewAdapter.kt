package com.krystianwsul.treeadapter

import android.support.annotation.LayoutRes
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

class TreeViewAdapter @JvmOverloads constructor(
        val treeModelAdapter: TreeModelAdapter,
        @param:LayoutRes @field:LayoutRes private val paddingLayout: Int? = null) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {

        private const val TYPE_PADDING = 1000
    }

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
        if (this.treeNodeCollection != null)
            throw SetTreeNodeCollectionCalledTwiceException()

        this.treeNodeCollection = treeNodeCollection
    }

    override fun getItemCount(): Int {
        if (treeNodeCollection == null)
            throw SetTreeNodeCollectionNotCalledException()

        return treeNodeCollection!!.displayedSize + if (paddingLayout != null) 1 else 0
    }

    fun hasActionMode() = treeModelAdapter.hasActionMode

    fun incrementSelected(x: TreeViewAdapter.Placeholder) = treeModelAdapter.incrementSelected(x)

    fun decrementSelected(x: TreeViewAdapter.Placeholder) = treeModelAdapter.decrementSelected(x)

    fun updateDisplayedNodes(action: () -> Unit) {
        if (treeNodeCollection == null)
            throw SetTreeNodeCollectionNotCalledException()

        check(!updating)

        val oldStates = treeNodeCollection!!.displayedNodes.map { it.state }

        updating = true
        action()
        updating = false

        val newStates = treeNodeCollection!!.displayedNodes.map { it.state }

        DiffUtil.calculateDiff(object : DiffUtil.Callback() {

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                if (paddingLayout != null) {
                    if (oldItemPosition == oldStates.size && newItemPosition == newStates.size)
                        return true
                    else if (oldItemPosition == oldStates.size || newItemPosition == newStates.size)
                        return false
                }

                return oldStates[oldItemPosition].modelState.same(newStates[newItemPosition].modelState)
            }

            override fun getOldListSize() = oldStates.size + (paddingLayout?.let { 1 } ?: 0)

            override fun getNewListSize() = newStates.size + (paddingLayout?.let { 1 } ?: 0)

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                if (paddingLayout != null) {
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

                return oldState.modelState == newState.modelState
            }
        }).dispatchUpdatesTo(this)
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
            checkNotNull(paddingLayout != null)
            check(position == itemCount - 1)
        }
    }

    override fun getItemViewType(position: Int): Int {
        if (treeNodeCollection == null)
            throw SetTreeNodeCollectionNotCalledException()

        return if (paddingLayout != null && position == treeNodeCollection!!.displayedSize)
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

    class SetTreeNodeCollectionNotCalledException : InitializationException("TreeViewAdapter.setTreeNodeCollection() has not been called.")

    class SetTreeNodeCollectionCalledTwiceException : InitializationException("TreeViewAdapter.setTreeNodeCollection() has already been called.")

    private class PaddingHolder(view: View) : RecyclerView.ViewHolder(view)

    object Placeholder
}
