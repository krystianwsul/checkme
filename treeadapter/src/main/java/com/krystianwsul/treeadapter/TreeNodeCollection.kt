package com.krystianwsul.treeadapter

import androidx.recyclerview.widget.RecyclerView
import java.util.*
import kotlin.math.ceil
import kotlin.math.min

class TreeNodeCollection<T : RecyclerView.ViewHolder>(val treeViewAdapter: TreeViewAdapter<T>) : NodeContainer<T> {

    private lateinit var treeNodes: MutableList<TreeNode<T>>

    val selectedNodes: List<TreeNode<T>>
        get() {
            if (!this::treeNodes.isInitialized)
                throw SetTreeNodesNotCalledException()

            return treeNodes.flatMap { it.selectedNodes }
        }

    var nodes: List<TreeNode<T>>
        get() {
            if (!this::treeNodes.isInitialized)
                throw SetTreeNodesNotCalledException()

            return treeNodes
        }
        set(rootTreeNodes) {
            if (this::treeNodes.isInitialized)
                throw SetTreeNodesCalledTwiceException()

            treeNodes = ArrayList(rootTreeNodes)
            treeNodes.sort()

            printOrdinals("setNodes")
        }

    private fun printOrdinals(prefix: String) {
        val lines = treeNodes.mapNotNull { it.modelNode.ordinalDesc()?.let { "ordinal $prefix $it" } }

        if (lines.isEmpty())
            return
    }

    fun getNode(position: Int): TreeNode<T> {
        if (!this::treeNodes.isInitialized)
            throw SetTreeNodesNotCalledException()

        var currPosition = position
        check(currPosition >= 0)
        check(currPosition < displayedSize)

        for (treeNode in treeNodes) {
            if (currPosition < treeNode.displayedSize)
                return treeNode.getNode(currPosition)

            currPosition -= treeNode.displayedSize
        }

        throw IndexOutOfBoundsException()
    }

    override fun getPosition(treeNode: TreeNode<T>): Int {
        if (!this::treeNodes.isInitialized)
            throw SetTreeNodesNotCalledException()

        var offset = 0
        for (currTreeNode in treeNodes) {
            val position = currTreeNode.getPosition(treeNode)
            if (position >= 0)
                return offset + position
            offset += currTreeNode.displayedSize
        }

        return -1
    }

    fun getItemViewType(position: Int): Int {
        val treeNode = getNode(position)

        return treeNode.itemViewType
    }

    override val displayedSize: Int
        get() {
            if (!this::treeNodes.isInitialized)
                throw SetTreeNodesNotCalledException()

            var displayedSize = 0
            for (treeNode in treeNodes)
                displayedSize += treeNode.displayedSize
            return displayedSize
        }

    val displayedNodes: List<TreeNode<T>>
        get() {
            if (!this::treeNodes.isInitialized)
                throw SetTreeNodesNotCalledException()

            return treeNodes.flatMap { it.displayedNodes }
        }

    fun unselect(x: TreeViewAdapter.Placeholder) {
        if (!this::treeNodes.isInitialized)
            throw SetTreeNodesNotCalledException()

        treeNodes.forEach { it.unselect(x) }
    }

    override fun add(treeNode: TreeNode<T>, x: TreeViewAdapter.Placeholder) {
        if (!this::treeNodes.isInitialized)
            throw SetTreeNodesNotCalledException()

        treeNodes.add(treeNode)
        treeNodes.sort()
    }

    override fun remove(treeNode: TreeNode<T>, x: TreeViewAdapter.Placeholder) {
        if (!this::treeNodes.isInitialized)
            throw SetTreeNodesNotCalledException()

        check(treeNodes.contains(treeNode))

        treeNodes.remove(treeNode)
    }

    override val isExpanded = true

    override val selectedChildren get() = selectedNodes

    override val treeNodeCollection = this

    fun selectAll(x: TreeViewAdapter.Placeholder) {
        if (!this::treeNodes.isInitialized)
            throw SetTreeNodesNotCalledException()

        treeNodes.forEach { it.selectAll(x) }
    }

    override val indentation = 0

    fun moveItem(fromPosition: Int, toPosition: Int, @Suppress("UNUSED_PARAMETER") x: TreeViewAdapter.Placeholder) {
        if (!this::treeNodes.isInitialized)
            throw SetTreeNodesNotCalledException()

        val treeNode = treeNodes[fromPosition]

        treeNodes.apply {
            removeAt(fromPosition)
            add(toPosition, treeNode)
        }
    }

    fun setNewItemPosition(position: Int) {
        if (!this::treeNodes.isInitialized)
            throw SetTreeNodesNotCalledException()

        val visibleNodes = treeNodes.filter { it.canBeShown() }

        val adjustedPosition = min(visibleNodes.size - 1, position) // padding

        val previousNode = adjustedPosition.takeIf { it > 0 }?.let { visibleNodes[adjustedPosition - 1] }
        val node = visibleNodes[adjustedPosition]
        val nextNode = adjustedPosition.takeIf { it < visibleNodes.size - 1 }?.let { visibleNodes[adjustedPosition + 1] }

        val previousOrdinal: Double
        val nextOrdinal: Double
        if (previousNode != null) {
            previousOrdinal = (previousNode.modelNode as Sortable).getOrdinal()

            val lastOrdinal = if (previousOrdinal.toInt() == ceil(previousOrdinal).toInt())
                previousOrdinal + 1
            else
                ceil(previousOrdinal)

            nextOrdinal = (nextNode?.modelNode as? Sortable)?.getOrdinal() ?: lastOrdinal
        } else {
            nextOrdinal = (nextNode!!.modelNode as Sortable).getOrdinal()
            previousOrdinal = nextOrdinal - 1000
        }

        printOrdinals("setNewItemPosition before")

        (node.modelNode as Sortable).setOrdinal((previousOrdinal + nextOrdinal) / 2)

        printOrdinals("setNewItemPosition after")
    }

    class SetTreeNodesNotCalledException : InitializationException("TreeNodeCollection.setTreeNodes() has not been called.")

    class SetTreeNodesCalledTwiceException : InitializationException("TreeNodeCollection.setTreeNodes() has already been called.")

    override val id = Id

    object Id
}
