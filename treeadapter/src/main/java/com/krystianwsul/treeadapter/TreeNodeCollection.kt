package com.krystianwsul.treeadapter

import com.jakewharton.rxrelay3.BehaviorRelay
import io.reactivex.rxjava3.core.Observable
import kotlin.math.ceil
import kotlin.math.min

class TreeNodeCollection<T : TreeHolder>(val treeViewAdapter: TreeViewAdapter<T>) : NodeContainer<T> {

    private val treeNodesRelay = BehaviorRelay.create<List<TreeNode<T>>>()

    val selectedNodes
        get() = treeNodesRelay.value
                ?.flatMap { it.selectedNodes }
                ?: throw SetTreeNodesNotCalledException()

    val nodesObservable: Observable<List<TreeNode<T>>> = treeNodesRelay

    var nodes
        get() = treeNodesRelay.value ?: throw SetTreeNodesNotCalledException()
        set(rootTreeNodes) {
            check(treeViewAdapter.locker == null)

            if (treeNodesRelay.value != null) throw SetTreeNodesCalledTwiceException()

            treeNodesRelay.accept(rootTreeNodes.sorted())

            printOrdinals("setNodes")
        }

    override val wantsSeparators = false

    private fun printOrdinals(prefix: String) = treeNodesRelay.value!!.mapNotNull {
        it.modelNode.ordinalDesc()?.let { "ordinal $prefix $it" }
    }

    override fun getNode(position: Int, positionMode: PositionMode): TreeNode<T> {
        check(position in positionMode.getRecursiveNodes(this).indices)

        val treeNodes = positionMode.getDirectChildNodes(this)
        var currPosition = position

        for (treeNode in treeNodes) {
            val nodeDisplayedSize = positionMode.getRecursiveNodes(treeNode).size

            if (currPosition < nodeDisplayedSize) return treeNode.getNode(currPosition, positionMode)

            currPosition -= nodeDisplayedSize
        }

        throw IndexOutOfBoundsException()
    }

    override fun getPosition(treeNode: TreeNode<T>, positionMode: PositionMode): Int {
        val treeNodes = positionMode.getDirectChildNodes(this)

        var offset = 0
        for (currTreeNode in treeNodes) {
            val position = currTreeNode.getPosition(treeNode, positionMode)
            if (position >= 0) return offset + position

            offset += positionMode.getRecursiveNodes(currTreeNode).size
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

    override val displayedChildNodes get() = displayedNodes

    override val displayableNodes
        get() = treeNodesRelay.value
                ?.flatMap { it.displayableNodes }
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

        check(treeViewAdapter.locker == null)

        treeNodesRelay.accept(treeNodes)
    }

    override fun remove(treeNode: TreeNode<T>, placeholder: TreeViewAdapter.Placeholder) {
        val treeNodes = treeNodesRelay.value
                ?.toMutableList()
                ?: throw SetTreeNodesNotCalledException()

        check(treeNodes.contains(treeNode))

        treeNodes.remove(treeNode)

        check(treeViewAdapter.locker == null)

        treeNodesRelay.accept(treeNodes)
    }

    override val isExpanded = true

    override val selectedChildren get() = selectedNodes

    override val treeNodeCollection = this

    fun selectAll(placeholder: TreeViewAdapter.Placeholder) = displayedNodes.forEach { it.select(placeholder) }

    override val indentation = 0

    fun moveItem(fromPosition: Int, toPosition: Int, placeholder: TreeViewAdapter.Placeholder) {
        val fromTreeNode = getNode(fromPosition, PositionMode.DISPLAYED)
        val toTreeNode = getNode(toPosition, PositionMode.DISPLAYED)

        listOf(fromTreeNode, toTreeNode).map { it.parent }
                .distinct()
                .single()
                .swapNodePositions(fromTreeNode, toTreeNode, placeholder)
    }

    override fun swapNodePositions(
            fromTreeNode: TreeNode<T>,
            toTreeNode: TreeNode<T>,
            placeholder: TreeViewAdapter.Placeholder,
    ) {
        check(treeViewAdapter.locker == null)

        val treeNodes = treeNodesRelay.value
                ?.toMutableList()
                ?: throw SetTreeNodesNotCalledException()

        val fromPosition = treeNodes.indexOf(fromTreeNode)
        check(fromPosition >= 0)

        val toPosition = treeNodes.indexOf(toTreeNode)
        check(toPosition >= 0)

        treeNodes.apply {
            removeAt(fromPosition)
            add(toPosition, fromTreeNode)
        }

        treeNodesRelay.accept(treeNodes)
    }

    fun setNewItemPosition(position: Int) {
        val visibleNodes = displayedNodes

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

    fun selectNode(position: Int) = treeViewAdapter.updateDisplayedNodes { displayedNodes[position].select(it) }

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
