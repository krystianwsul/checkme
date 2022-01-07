package com.krystianwsul.treeadapter

import android.util.Log
import com.jakewharton.rxrelay3.BehaviorRelay
import com.krystianwsul.common.criteria.SearchCriteria
import io.reactivex.rxjava3.core.Observable
import kotlin.math.*

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

    override fun wantsSeparators(top: Boolean) = false

    override fun getNode(position: Int, positionMode: PositionMode): TreeNode<T> {
        check(position >= 0)

        return positionMode.getRecursiveNodes(this)[position]
    }

    override fun getPosition(positionMode: PositionMode, matcher: (TreeNode<T>) -> Boolean): Int {
        val treeNodes = positionMode.getDirectChildNodes(this)

        var offset = 0
        for (currTreeNode in treeNodes) {
            val position = currTreeNode.getPosition(positionMode, matcher)
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

    override val displayedDirectChildNodes get() = nodes.filter { it.visible() }

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

        val fromParent = fromTreeNode.parent
        val toParent = toTreeNode.parent

        if (fromParent == toParent) {
            fromParent.swapNodePositions(fromTreeNode, toTreeNode, placeholder)
        } else {
            fromParent.removeForSwap(fromTreeNode, placeholder)
            toParent.addForSwap(fromTreeNode, toTreeNode, placeholder)
        }
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

    override fun removeForSwap(fromTreeNode: TreeNode<T>, placeholder: TreeViewAdapter.Placeholder) {
        check(treeViewAdapter.locker == null)

        val treeNodes = treeNodesRelay.value
            ?.toMutableList()
            ?: throw SetTreeNodesNotCalledException()

        val fromPosition = treeNodes.indexOf(fromTreeNode)
        check(fromPosition >= 0)

        treeNodes.removeAt(fromPosition)

        treeNodesRelay.accept(treeNodes)
    }

    override fun addForSwap(fromTreeNode: TreeNode<T>, toTreeNode: TreeNode<T>, placeholder: TreeViewAdapter.Placeholder) {
        check(treeViewAdapter.locker == null)

        val treeNodes = treeNodesRelay.value
            ?.toMutableList()
            ?: throw SetTreeNodesNotCalledException()

        val toPosition = treeNodes.indexOf(toTreeNode)
        check(toPosition >= 0)

        fromTreeNode.parent = this
        treeNodes.add(toPosition, fromTreeNode)

        treeNodesRelay.accept(treeNodes)
    }

    private fun getAdjacentNodes(currentTreeNode: TreeNode<*>): Pair<Sortable?, Sortable?> {
        val displayedNodesInParent = currentTreeNode.parent.displayedDirectChildNodes
        val currentPositionInParent = displayedNodesInParent.indexOf(currentTreeNode)

        val previousNode = currentPositionInParent.takeIf { it > 0 }
            ?.let { displayedNodesInParent[currentPositionInParent - 1] }
            ?.modelNode
            ?.let { it as? Sortable }
            ?.takeIf { it.sortable }

        val nextNode = currentPositionInParent.takeIf { it < displayedNodesInParent.size - 1 }
            ?.let { displayedNodesInParent[currentPositionInParent + 1] }
            ?.modelNode
            ?.let { it as? Sortable }
            ?.takeIf { it.sortable }

        val reversedOrdinal = listOfNotNull(previousNode, nextNode).map { it.reversedOrdinal }
            .distinct()
            .single()

        return if (reversedOrdinal)
            nextNode to previousNode
        else
            previousNode to nextNode
    }

    fun setNewItemPosition(position: Int) {
        val allDisplayedNodes = displayedNodes
        val adjustedPositionInAllNodes = min(allDisplayedNodes.size - 1, position) // padding
        val currentTreeNode = allDisplayedNodes[adjustedPositionInAllNodes]
        val currentNode = currentTreeNode.modelNode

        val (previousNode, nextNode) = getAdjacentNodes(currentTreeNode)

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

            /*
            ordinal double hack

            previous code:
            previousOrdinal = nextOrdinal - 1000

            if this keeps causing issues, throw in the towel, create a new ordinalStr field in FB, store BigDecimal in it,
            and do a migration
             */
            previousOrdinal = nextOrdinal - max(1000.0, abs(nextOrdinal) / 10)
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

    fun expandMatching(search: SearchCriteria.Search) {
        treeNodesRelay.value!!.forEach { it.expandMatching(search) }
    }

    class SetTreeNodesNotCalledException : InitializationException("TreeNodeCollection.setTreeNodes() has not been called.")

    class SetTreeNodesCalledTwiceException :
        InitializationException("TreeNodeCollection.setTreeNodes() has already been called.")

    override val id = Id

    object Id
}
