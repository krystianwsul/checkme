package com.krystianwsul.treeadapter

import androidx.recyclerview.widget.RecyclerView
import com.jakewharton.rxrelay2.BehaviorRelay
import io.reactivex.Observable
import kotlin.math.ceil
import kotlin.math.min

class TreeNodeCollection<T : RecyclerView.ViewHolder>(val treeViewAdapter: TreeViewAdapter<T>) : NodeContainer<T> {

    private val treeNodesRelay = BehaviorRelay.create<List<TreeNode<T>>>()

    val selectedNodes
        get() = treeNodesRelay.value
                ?.flatMap { it.selectedNodes }
                ?: throw SetTreeNodesNotCalledException()

    val nodesObservable: Observable<List<TreeNode<T>>> = treeNodesRelay

    var nodes
        get() = treeNodesRelay.value ?: throw SetTreeNodesNotCalledException()
        set(rootTreeNodes) {
            if (treeNodesRelay.value != null) throw SetTreeNodesCalledTwiceException()

            treeNodesRelay.accept(rootTreeNodes.sorted())

            printOrdinals("setNodes")
        }

    override val wantsSeparators = false

    private fun printOrdinals(prefix: String) = treeNodesRelay.value!!.mapNotNull {
        it.modelNode.ordinalDesc()?.let { "ordinal $prefix $it" }
    }

    fun getNode(position: Int): TreeNode<T> {
        val treeNodes = treeNodesRelay.value ?: throw SetTreeNodesNotCalledException()

        var currPosition = position
        check(currPosition >= 0)
        check(currPosition < displayedNodes.size)

        for (treeNode in treeNodes) {
            val nodeDisplayedSize = treeNode.displayedNodes.size

            if (currPosition < nodeDisplayedSize)
                return treeNode.getNode(currPosition)

            currPosition -= nodeDisplayedSize
        }

        throw IndexOutOfBoundsException()
    }

    override fun getPosition(treeNode: TreeNode<T>): Int {
        val treeNodes = treeNodesRelay.value ?: throw SetTreeNodesNotCalledException()

        var offset = 0
        for (currTreeNode in treeNodes) {
            val position = currTreeNode.getPosition(treeNode)
            if (position >= 0)
                return offset + position
            offset += currTreeNode.displayedNodes.size
        }

        return -1
    }

    fun getItemViewType(position: Int): Int {
        val treeNode = getNode(position)

        return treeNode.itemViewType
    }

    override val displayedNodes
        get() = treeNodesRelay.value
                ?.flatMap { it.displayedNodes }
                ?: throw SetTreeNodesNotCalledException()

    fun unselect(x: TreeViewAdapter.Placeholder) = treeNodesRelay.value
            ?.forEach { it.unselect(x) }
            ?: throw SetTreeNodesNotCalledException()

    override fun add(treeNode: TreeNode<T>, placeholder: TreeViewAdapter.Placeholder) {
        val treeNodes = treeNodesRelay.value
                ?.toMutableList()
                ?: throw SetTreeNodesNotCalledException()

        treeNodes.add(treeNode)
        treeNodes.sort()

        treeNodesRelay.accept(treeNodes)
    }

    override fun remove(treeNode: TreeNode<T>, placeholder: TreeViewAdapter.Placeholder) {
        val treeNodes = treeNodesRelay.value
                ?.toMutableList()
                ?: throw SetTreeNodesNotCalledException()

        check(treeNodes.contains(treeNode))

        treeNodes.remove(treeNode)

        treeNodesRelay.accept(treeNodes)
    }

    override val isExpanded = true

    override val selectedChildren get() = selectedNodes

    override val treeNodeCollection = this

    fun selectAll(placeholder: TreeViewAdapter.Placeholder) = displayedNodes.forEach { it.select(placeholder) }

    override val indentation = 0

    fun moveItem(fromPosition: Int, toPosition: Int, @Suppress("UNUSED_PARAMETER") x: TreeViewAdapter.Placeholder) {
        val treeNodes = treeNodesRelay.value
                ?.toMutableList()
                ?: throw SetTreeNodesNotCalledException()

        val treeNode = treeNodes[fromPosition]

        treeNodes.apply {
            removeAt(fromPosition)
            add(toPosition, treeNode)
        }

        treeNodesRelay.accept(treeNodes)
    }

    fun setNewItemPosition(position: Int) {
        val treeNodes = treeNodesRelay.value ?: throw SetTreeNodesNotCalledException()

        val visibleNodes = treeNodes.filter { it.canBeShown() }

        val adjustedPosition = min(visibleNodes.size - 1, position) // padding

        val previousNode = adjustedPosition.takeIf { it > 0 }
                ?.let { visibleNodes[adjustedPosition - 1] }
                ?.modelNode
                as? Sortable

        val currentNode = visibleNodes[adjustedPosition].modelNode as Sortable

        val nextNode = adjustedPosition.takeIf { it < visibleNodes.size - 1 }
                ?.let { visibleNodes[adjustedPosition + 1] }
                ?.modelNode
                as? Sortable

        check(previousNode != null || nextNode != null)

        val previousOrdinal: Double
        val nextOrdinal: Double
        if (previousNode != null) {
            previousOrdinal = previousNode.getOrdinal()

            val lastOrdinal = if (previousOrdinal.toInt() == ceil(previousOrdinal).toInt())
                previousOrdinal + 1
            else
                ceil(previousOrdinal)

            nextOrdinal = nextNode?.getOrdinal() ?: lastOrdinal
        } else {
            nextOrdinal = (nextNode as Sortable).getOrdinal()
            previousOrdinal = nextOrdinal - 1000
        }

        printOrdinals("setNewItemPosition before")

        currentNode.setOrdinal((previousOrdinal + nextOrdinal) / 2)

        printOrdinals("setNewItemPosition after")
    }

    fun selectNode(position: Int) {
        val treeNodes = treeNodesRelay.value ?: throw SetTreeNodesNotCalledException()

        treeViewAdapter.updateDisplayedNodes { treeNodes[position].select(it) }
    }

    fun normalize() = treeNodesRelay.value
            ?.forEach { it.normalize() }
            ?: throw SetTreeNodesNotCalledException()

    fun collapseAll() {
        treeNodesRelay.value!!.forEach { it.collapseAll() }
    }

    fun expandMatching(query: String) {
        treeNodesRelay.value!!.forEach { it.expandMatching(query) }
    }

    class SetTreeNodesNotCalledException : InitializationException("TreeNodeCollection.setTreeNodes() has not been called.")

    class SetTreeNodesCalledTwiceException : InitializationException("TreeNodeCollection.setTreeNodes() has already been called.")

    override val id = Id

    object Id
}
