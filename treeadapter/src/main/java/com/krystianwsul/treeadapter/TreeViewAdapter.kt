package com.krystianwsul.treeadapter

import android.support.annotation.LayoutRes
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

    fun setTreeNodeCollection(treeNodeCollection: TreeNodeCollection) {
        if (this.treeNodeCollection != null)
            throw SetTreeNodeCollectionCalledTwiceException()

        this.treeNodeCollection = treeNodeCollection
    }

    override fun getItemCount(): Int {
        if (treeNodeCollection == null)
            throw SetTreeNodeCollectionNotCalledException()

        return treeNodeCollection!!.displayedSize() + if (paddingLayout != null) 1 else 0
    }

    fun hasActionMode() = treeModelAdapter.hasActionMode()

    fun incrementSelected() = treeModelAdapter.incrementSelected()

    fun decrementSelected() = treeModelAdapter.decrementSelected()

    fun onCreateActionMode() {
        if (treeNodeCollection == null)
            throw SetTreeNodeCollectionNotCalledException()

        treeNodeCollection!!.onCreateActionMode()
    }

    fun onDestroyActionMode() {
        if (treeNodeCollection == null)
            throw SetTreeNodeCollectionNotCalledException()

        treeNodeCollection!!.onDestroyActionMode()
    }

    fun unselect() {
        if (treeNodeCollection == null)
            throw SetTreeNodeCollectionNotCalledException()

        treeNodeCollection!!.unselect()
    }

    fun selectAll() {
        if (treeNodeCollection == null)
            throw SetTreeNodeCollectionNotCalledException()

        check(!treeModelAdapter.hasActionMode())

        treeNodeCollection!!.selectAll()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_PADDING) {
            checkNotNull(paddingLayout)

            PaddingHolder(LayoutInflater.from(parent.context).inflate(paddingLayout!!, parent, false)!!)
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

        val displayedSize = treeNodeCollection!!.displayedSize()

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

        return if (paddingLayout != null && position == treeNodeCollection!!.displayedSize())
            TYPE_PADDING
        else
            treeNodeCollection!!.getItemViewType(position)
    }

    fun moveItem(from: Int, to: Int) {
        if (treeNodeCollection == null)
            throw SetTreeNodeCollectionNotCalledException()

        treeNodeCollection!!.moveItem(from, to)
    }

    fun setNewItemPosition(position: Int) {
        if (treeNodeCollection == null)
            throw SetTreeNodeCollectionNotCalledException()

        treeNodeCollection!!.setNewItemPosition(position)
    }

    class SetTreeNodeCollectionNotCalledException() : InitializationException("TreeViewAdapter.setTreeNodeCollection() has not been called.")

    class SetTreeNodeCollectionCalledTwiceException() : InitializationException("TreeViewAdapter.setTreeNodeCollection() has already been called.")

    private class PaddingHolder(view: View) : RecyclerView.ViewHolder(view)
}
