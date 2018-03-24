package com.krystianwsul.treeadapter

import android.util.Log
import com.annimon.stream.Collectors
import java.util.*

class TreeNodeCollection(val mTreeViewAdapter: TreeViewAdapter) : NodeContainer {

    private var mTreeNodes: MutableList<TreeNode>? = null

    val selectedNodes: List<TreeNode>
        get() {
            if (mTreeNodes == null)
                throw SetTreeNodesNotCalledException()

            return mTreeNodes!!.flatMap { it.selectedNodes.collect(Collectors.toList()) }
        }

    var nodes: List<TreeNode>
        get() {
            if (mTreeNodes == null)
                throw SetTreeNodesNotCalledException()

            return mTreeNodes!!
        }
        set(rootTreeNodes) {
            if (mTreeNodes != null)
                throw SetTreeNodesCalledTwiceException()

            mTreeNodes = ArrayList(rootTreeNodes)

            mTreeNodes!!.sort()
        }

    fun getNode(position: Int): TreeNode {
        if (mTreeNodes == null)
            throw SetTreeNodesNotCalledException()

        var currPosition = position
        check(currPosition >= 0)
        check(currPosition < displayedSize())

        for (treeNode in mTreeNodes!!) {
            if (currPosition < treeNode.displayedSize())
                return treeNode.getNode(currPosition)

            currPosition -= treeNode.displayedSize()
        }

        throw IndexOutOfBoundsException()
    }

    override fun getPosition(treeNode: TreeNode): Int {
        if (mTreeNodes == null)
            throw SetTreeNodesNotCalledException()

        var offset = 0
        for (currTreeNode in mTreeNodes!!) {
            val position = currTreeNode.getPosition(treeNode)
            if (position >= 0)
                return offset + position
            offset += currTreeNode.displayedSize()
        }

        return -1
    }

    fun getItemViewType(position: Int): Int {
        val treeNode = getNode(position)

        return treeNode.itemViewType
    }

    override fun displayedSize(): Int {
        if (mTreeNodes == null)
            throw SetTreeNodesNotCalledException()

        var displayedSize = 0
        for (treeNode in mTreeNodes!!)
            displayedSize += treeNode.displayedSize()
        return displayedSize
    }

    fun onCreateActionMode() {
        if (mTreeNodes == null)
            throw SetTreeNodesNotCalledException()

        mTreeNodes!!.forEach(TreeNode::onCreateActionMode)
    }

    fun onDestroyActionMode() {
        if (mTreeNodes == null)
            throw SetTreeNodesNotCalledException()

        mTreeNodes!!.forEach(TreeNode::onDestroyActionMode)
    }

    fun unselect() {
        if (mTreeNodes == null)
            throw SetTreeNodesNotCalledException()

        mTreeNodes!!.forEach(TreeNode::unselect)
    }

    override fun add(treeNode: TreeNode) {
        if (mTreeNodes == null)
            throw SetTreeNodesNotCalledException()

        mTreeNodes!!.add(treeNode)

        mTreeNodes!!.sort()

        val treeViewAdapter = mTreeViewAdapter

        val newPosition = getPosition(treeNode)
        check(newPosition >= 0)

        treeViewAdapter.notifyItemInserted(newPosition)

        if (newPosition > 0)
            treeViewAdapter.notifyItemChanged(newPosition - 1)
    }

    override fun remove(treeNode: TreeNode) {
        if (mTreeNodes == null)
            throw SetTreeNodesNotCalledException()

        check(mTreeNodes!!.contains(treeNode))

        val treeViewAdapter = mTreeViewAdapter

        val oldPosition = getPosition(treeNode)
        check(oldPosition >= 0)

        val displayedSize = treeNode.displayedSize()

        mTreeNodes!!.remove(treeNode)

        treeViewAdapter.notifyItemRangeRemoved(oldPosition, displayedSize)

        if (oldPosition > 0)
            treeViewAdapter.notifyItemChanged(oldPosition - 1)
    }

    override fun expanded() = true

    override fun update() = Unit

    override fun updateRecursive() = Unit

    override fun getSelectedChildren() = selectedNodes

    override fun getTreeNodeCollection() = this

    fun selectAll() {
        if (mTreeNodes == null)
            throw SetTreeNodesNotCalledException()

        mTreeNodes!!.forEach(TreeNode::selectAll)
    }

    override fun getIndentation() = 0

    fun moveItem(fromPosition: Int, toPosition: Int) {
        if (mTreeNodes == null)
            throw SetTreeNodesNotCalledException()

        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                Collections.swap(mTreeNodes!!, i, i + 1)
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                Collections.swap(mTreeNodes!!, i, i - 1)
            }
        }
        mTreeViewAdapter.notifyItemMoved(fromPosition, toPosition)
    }

    fun setNewItemPosition(position: Int) {
        // todo

        Log.e("asdf", "setNewItemPosition $position")
    }

    class SetTreeNodesNotCalledException : InitializationException("TreeNodeCollection.setTreeNodes() has not been called.")

    class SetTreeNodesCalledTwiceException : InitializationException("TreeNodeCollection.setTreeNodes() has already been called.")
}
