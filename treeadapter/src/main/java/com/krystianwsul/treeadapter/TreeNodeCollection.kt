package com.krystianwsul.treeadapter

import java.util.*

class TreeNodeCollection(val mTreeViewAdapter: TreeViewAdapter) : NodeContainer {

    private var treeNodes: MutableList<TreeNode>? = null

    val selectedNodes: List<TreeNode>
        get() {
            if (treeNodes == null)
                throw SetTreeNodesNotCalledException()

            return treeNodes!!.flatMap { it.selectedNodes }
        }

    var nodes: List<TreeNode>
        get() {
            if (treeNodes == null)
                throw SetTreeNodesNotCalledException()

            return treeNodes!!
        }
        set(rootTreeNodes) {
            if (treeNodes != null)
                throw SetTreeNodesCalledTwiceException()

            treeNodes = ArrayList(rootTreeNodes)

            treeNodes!!.sort()
        }

    fun getNode(position: Int): TreeNode {
        if (treeNodes == null)
            throw SetTreeNodesNotCalledException()

        var currPosition = position
        check(currPosition >= 0)
        check(currPosition < displayedSize())

        for (treeNode in treeNodes!!) {
            if (currPosition < treeNode.displayedSize())
                return treeNode.getNode(currPosition)

            currPosition -= treeNode.displayedSize()
        }

        throw IndexOutOfBoundsException()
    }

    override fun getPosition(treeNode: TreeNode): Int {
        if (treeNodes == null)
            throw SetTreeNodesNotCalledException()

        var offset = 0
        for (currTreeNode in treeNodes!!) {
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
        if (treeNodes == null)
            throw SetTreeNodesNotCalledException()

        var displayedSize = 0
        for (treeNode in treeNodes!!)
            displayedSize += treeNode.displayedSize()
        return displayedSize
    }

    val displayedNodes: List<TreeNode>
        get() {
            if (treeNodes == null)
                throw SetTreeNodesNotCalledException()

            return treeNodes!!.flatMap { it.displayedNodes }
        }

    fun unselect() {
        if (treeNodes == null)
            throw SetTreeNodesNotCalledException()

        treeNodes!!.forEach(TreeNode::unselect)
    }

    override fun add(treeNode: TreeNode) {
        if (treeNodes == null)
            throw SetTreeNodesNotCalledException()

        treeNodes!!.add(treeNode)

        treeNodes!!.sort()

        val treeViewAdapter = mTreeViewAdapter

        val newPosition = getPosition(treeNode)
        check(newPosition >= 0)

        treeViewAdapter.notifyItemInserted(newPosition)

        if (newPosition > 0)
            treeViewAdapter.notifyItemChanged(newPosition - 1)
    }

    override fun remove(treeNode: TreeNode) {
        if (treeNodes == null)
            throw SetTreeNodesNotCalledException()

        check(treeNodes!!.contains(treeNode))

        val treeViewAdapter = mTreeViewAdapter

        val oldPosition = getPosition(treeNode)
        check(oldPosition >= 0)

        val displayedSize = treeNode.displayedSize()

        treeNodes!!.remove(treeNode)

        treeViewAdapter.notifyItemRangeRemoved(oldPosition, displayedSize)

        if (oldPosition > 0)
            treeViewAdapter.notifyItemChanged(oldPosition - 1)
    }

    override fun isExpanded() = true

    override fun update() = Unit

    override fun updateRecursive() = Unit

    override fun getSelectedChildren() = selectedNodes

    override fun getTreeNodeCollection() = this

    fun selectAll() {
        if (treeNodes == null)
            throw SetTreeNodesNotCalledException()

        treeNodes!!.forEach(TreeNode::selectAll)
    }

    override fun getIndentation() = 0

    fun moveItem(fromPosition: Int, toPosition: Int) {
        if (treeNodes == null)
            throw SetTreeNodesNotCalledException()

        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                Collections.swap(treeNodes!!, i, i + 1)
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                Collections.swap(treeNodes!!, i, i - 1)
            }
        }
        mTreeViewAdapter.notifyItemMoved(fromPosition, toPosition)
    }

    fun setNewItemPosition(position: Int) {
        if (treeNodes == null)
            throw SetTreeNodesNotCalledException()

        treeNodes!!.let { treeNodes ->
            val visibleNodes = treeNodes.filter { it.canBeShown() }

            val previousNode = position.takeIf { it > 0 }?.let { visibleNodes[position - 1] }
            val node = visibleNodes[position]
            val nextNode = position.takeIf { it < visibleNodes.size - 1 }?.let { visibleNodes[position + 1] }

            val previousOrdinal = previousNode?.modelNode?.getOrdinal() ?: -Double.MAX_VALUE
            val nextOrdinal = nextNode?.modelNode?.getOrdinal() ?: Double.MAX_VALUE

            node.modelNode.setOrdinal((previousOrdinal + nextOrdinal) / 2)
        }
    }

    class SetTreeNodesNotCalledException : InitializationException("TreeNodeCollection.setTreeNodes() has not been called.")

    class SetTreeNodesCalledTwiceException : InitializationException("TreeNodeCollection.setTreeNodes() has already been called.")
}
