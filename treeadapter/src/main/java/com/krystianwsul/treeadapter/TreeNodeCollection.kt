package com.krystianwsul.treeadapter

import android.util.Log
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
        }

    override val wantsSeparators = false

    override fun getNode(position: Int, positionMode: PositionMode): TreeNode<T> {
        Log.e("asdf", "magic positionMode: $positionMode") // todo project
        return positionMode.getRecursiveNodes(this)[position]
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

    override val displayedNodes get() = nodes.flatMap { it.displayedNodes }

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
        val allDisplayedNodes = displayedNodes
        val adjustedPositionInAllNodes = min(allDisplayedNodes.size - 1, position) // padding
        val currentTreeNode = allDisplayedNodes[adjustedPositionInAllNodes]
        val currentNode = currentTreeNode.modelNode

        val displayedNodesInParent = currentTreeNode.parent.displayedChildNodes
        val currentPositionInParent = displayedNodesInParent.indexOf(currentTreeNode)

        val previousNode = currentPositionInParent.takeIf { it > 0 }
                ?.let { displayedNodesInParent[currentPositionInParent - 1] }
                ?.modelNode
                as? Sortable

        val nextNode = currentPositionInParent.takeIf { it < displayedNodesInParent.size - 1 }
                ?.let { displayedNodesInParent[currentPositionInParent + 1] }
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

        (currentNode as Sortable).setOrdinal((previousOrdinal + nextOrdinal) / 2)
    }

    fun selectNode(position: Int) = treeViewAdapter.updateDisplayedNodes { displayedNodes[position].select(it) }

    fun normalize() = treeNodesRelay.value
            ?.forEach { it.normalize() }
            ?: throw SetTreeNodesNotCalledException()

    fun resetExpansion(onlyProgrammatic: Boolean, placeholder: TreeViewAdapter.Placeholder) {
        treeNodesRelay.value!!.forEach { it.resetExpansion(onlyProgrammatic, placeholder) }
    }

    fun expandMatching(query: String, force: Boolean) {
        treeNodesRelay.value!!.forEach { it.expandMatching(query, force) }
    }

    class SetTreeNodesNotCalledException : InitializationException("TreeNodeCollection.setTreeNodes() has not been called.")

    class SetTreeNodesCalledTwiceException : InitializationException("TreeNodeCollection.setTreeNodes() has already been called.")

    override val id = Id

    object Id
}
