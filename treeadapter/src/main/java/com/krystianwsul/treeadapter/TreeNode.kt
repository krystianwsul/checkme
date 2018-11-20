package com.krystianwsul.treeadapter

import android.support.v7.widget.RecyclerView
import android.view.View
import java.util.*

class TreeNode(
        val modelNode: ModelNode,
        val parent: NodeContainer,
        private var expanded: Boolean,
        private var selected: Boolean) : Comparable<TreeNode>, NodeContainer {

    override val isExpanded get() = expanded

    private var childTreeNodes: MutableList<TreeNode>? = null

    val itemViewType = modelNode.itemViewType

    val onLongClickListener
        get() = View.OnLongClickListener {
            this@TreeNode.onLongClick()
            true
        }

    val onClickListener
        get() = View.OnClickListener {
            if (hasActionMode()) {
                onLongClick()
            } else {
                modelNode.onClick()
            }
        }

    private val treeViewAdapter by lazy { treeNodeCollection.mTreeViewAdapter }

    val isSelected: Boolean
        get() {
            check(!selected || modelNode.isSelectable)

            return selected
        }

    val selectedNodes: List<TreeNode>
        get() {
            if (childTreeNodes == null)
                throw SetChildTreeNodesNotCalledException()

            check(!selected || modelNode.isSelectable)

            val selectedTreeNodes = ArrayList<TreeNode>()

            if (selected) {
                check(modelNode.isVisibleDuringActionMode)
                selectedTreeNodes.add(this)
            }

            selectedTreeNodes.addAll(childTreeNodes!!.flatMap { it.selectedNodes })

            return selectedTreeNodes
        }

    val expandVisible: Boolean
        get() {
            if (childTreeNodes == null)
                throw SetChildTreeNodesNotCalledException()

            if (!visible())
                throw InvisibleNodeException()

            if (childTreeNodes!!.isEmpty())
                return false

            if (childTreeNodes!!.none { it.canBeShown() })
                return false

            return !(hasActionMode() && hasSelectedDescendants())
        }

    val state get() = State(isExpanded, isSelected, separatorVisibility, modelNode.state)

    // hiding
    // showing
    val expandListener: View.OnClickListener
        get() {
            if (childTreeNodes == null)
                throw SetChildTreeNodesNotCalledException()

            return View.OnClickListener {
                if (childTreeNodes!!.isEmpty())
                    throw EmptyExpandedException()

                val treeNodeCollection = treeNodeCollection

                if (hasSelectedDescendants() && hasActionMode())
                    throw DescendantSelectedException()

                val position = treeNodeCollection.getPosition(this@TreeNode)
                check(position >= 0)

                if (expanded) {
                    if (hasSelectedDescendants())
                        throw SelectedChildrenException()

                    val displayedSize = displayedSize
                    expanded = false // todo remove remaining .notifyItem
                    treeViewAdapter.notifyItemRangeRemoved(position + 1, displayedSize - 1)
                } else {
                    expanded = true
                    treeViewAdapter.notifyItemRangeInserted(position + 1, displayedSize - 1)
                }

                if (position > 0) {
                    treeViewAdapter.notifyItemRangeChanged(position - 1, 2)
                } else {
                    treeViewAdapter.notifyItemChanged(position)
                }
            }
        }

    val separatorVisibility: Boolean
        get() {
            if (!parent.isExpanded)
                throw InvisibleNodeException()

            val positionInCollection = treeNodeCollection.getPosition(this)
            check(positionInCollection >= 0)

            if (positionInCollection == treeNodeCollection.displayedSize - 1)
                return false

            if (parent.getPosition(this) == parent.displayedSize - 1)
                return true

            val nextTreeNode = treeNodeCollection.getNode(positionInCollection + 1)

            return nextTreeNode.isExpanded || modelNode.isSeparatorVisibleWhenNotExpanded
        }

    val allChildren: List<TreeNode>
        get() {
            if (childTreeNodes == null)
                throw SetChildTreeNodesNotCalledException()

            return childTreeNodes!!
        }

    init {
        if (selected && !modelNode.isSelectable)
            throw NotSelectableSelectedException()

        if (modelNode.isSelectable && !modelNode.isVisibleDuringActionMode)
            throw SelectableNotVisibleException()
    }

    fun setChildTreeNodes(childTreeNodes: List<TreeNode>) {
        if (this.childTreeNodes != null)
            throw SetChildTreeNodesCalledTwiceException()

        if (expanded && childTreeNodes.isEmpty())
            throw EmptyExpandedException()

        this.childTreeNodes = ArrayList(childTreeNodes)

        this.childTreeNodes!!.sort()
    }

    fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder) = modelNode.onBindViewHolder(viewHolder)

    override fun update() = treeViewAdapter.notifyItemChanged(treeNodeCollection.getPosition(this))

    override fun updateRecursive() {
        update()

        parent.updateRecursive()
    }

    override fun compareTo(other: TreeNode) = modelNode.compareTo(other.modelNode)

    private fun onLongClick() {
        if (!modelNode.isSelectable)
            return

        selected = !selected

        if (selected) {
            incrementSelected()

            if (parent.selectedChildren.size == 1)
            // first in group
                parent.updateRecursive()
        } else {
            decrementSelected()

            if (parent.selectedChildren.isEmpty())// last in group
                parent.updateRecursive()
        }

        treeViewAdapter.notifyItemChanged(treeNodeCollection.getPosition(this))
    }

    private fun hasActionMode() = treeViewAdapter.hasActionMode()

    private fun incrementSelected() = treeViewAdapter.incrementSelected()

    private fun decrementSelected() = treeViewAdapter.decrementSelected()

    override val displayedSize: Int
        get() {
            if (childTreeNodes == null)
                throw SetChildTreeNodesNotCalledException()

            return if ((!modelNode.isVisibleWhenEmpty && childTreeNodes!!.isEmpty()) || (!modelNode.isVisibleDuringActionMode && hasActionMode()) || !modelNode.matchesSearch(treeViewAdapter.query)) {
                0
            } else {
                1 + if (expanded) childTreeNodes!!.map { it.displayedSize }.sum() else 0
            }
        }

    val displayedNodes: List<TreeNode>
        get() {
            if (childTreeNodes == null)
                throw SetChildTreeNodesNotCalledException()

            return if (!modelNode.isVisibleWhenEmpty && childTreeNodes!!.isEmpty() || !modelNode.isVisibleDuringActionMode && hasActionMode() || !modelNode.matchesSearch(treeViewAdapter.query)) {
                listOf()
            } else {
                if (expanded)
                    listOf(this) + childTreeNodes!!.flatMap { it.displayedNodes }
                else
                    listOf(this)
            }
        }

    fun getNode(position: Int): TreeNode {
        var currPosition = position
        if (childTreeNodes == null)
            throw SetChildTreeNodesNotCalledException()

        check(currPosition >= 0)
        check(currPosition < displayedSize)

        if (currPosition == 0)
            return this

        check(expanded)

        currPosition--

        for (notDoneGroupTreeNode in childTreeNodes!!) {
            if (currPosition < notDoneGroupTreeNode.displayedSize)
                return notDoneGroupTreeNode.getNode(currPosition)

            currPosition -= notDoneGroupTreeNode.displayedSize
        }

        throw IndexOutOfBoundsException()
    }

    override fun getPosition(treeNode: TreeNode): Int {
        if (childTreeNodes == null)
            throw SetChildTreeNodesNotCalledException()

        if (treeNode === this)
            return 0

        if (!expanded)
            return -1

        var offset = 1
        for (childTreeNode in childTreeNodes!!) {
            val position = childTreeNode.getPosition(treeNode)
            if (position >= 0)
                return offset + position
            offset += childTreeNode.displayedSize
        }

        return -1
    }

    fun unselect() {
        check(!selected || modelNode.isSelectable)

        if (selected) {
            check(modelNode.isSelectable)
            check(modelNode.isVisibleDuringActionMode)

            selected = false
            treeViewAdapter.notifyItemChanged(treeNodeCollection.getPosition(this))
        }

        val selected = selectedNodes

        if (!selected.isEmpty()) {
            check(expanded)

            selected.forEach { it.unselect() }

            treeViewAdapter.notifyItemChanged(treeNodeCollection.getPosition(this))
        }
    }

    fun selectAll() {
        if (childTreeNodes == null)
            throw SetChildTreeNodesNotCalledException()

        if (selected)
            throw SelectAllException()

        val treeNodeCollection = treeNodeCollection

        if (modelNode.isSelectable) {
            check(modelNode.isVisibleDuringActionMode)

            selected = true

            treeViewAdapter.notifyItemChanged(treeNodeCollection.getPosition(this))

            treeViewAdapter.incrementSelected()
        }

        if (expanded) {
            check(!childTreeNodes!!.isEmpty())

            childTreeNodes!!.forEach { it.selectAll() }
        }
    }

    fun canBeShown(): Boolean {
        if (childTreeNodes == null)
            throw SetChildTreeNodesNotCalledException()

        if (!modelNode.isVisibleDuringActionMode && hasActionMode())
            return false

        if (!modelNode.matchesSearch(treeViewAdapter.query))
            return false

        return !(!modelNode.isVisibleWhenEmpty && childTreeNodes!!.isEmpty())
    }

    private fun visible(): Boolean {
        if (childTreeNodes == null)
            throw SetChildTreeNodesNotCalledException()

        if (!canBeShown())
            return false

        return if (parent is TreeNodeCollection) {
            true
        } else {
            (parent as TreeNode).visible() && parent.isExpanded
        }
    }

    override fun remove(treeNode: TreeNode, x: Any) { // todo remove x
        if (childTreeNodes == null)
            throw SetChildTreeNodesNotCalledException()

        if (!childTreeNodes!!.contains(treeNode))
            throw NoSuchNodeException()

        val visible = visible()

        childTreeNodes!!.remove(treeNode)

        if (!visible)
            return

        if (expanded) {
            if (0 == childTreeNodes!!.map { it.displayedSize }.sum())
                expanded = false
        }
    }

    fun removeAll(x: Any) {
        if (childTreeNodes == null)
            throw SetChildTreeNodesNotCalledException()

        ArrayList(childTreeNodes!!).forEach { remove(it, x) }
    }

    override fun add(treeNode: TreeNode, x: Any) {
        if (childTreeNodes == null)
            throw SetChildTreeNodesNotCalledException()

        childTreeNodes!!.add(treeNode)
        childTreeNodes!!.sort()
    }

    override val selectedChildren: List<TreeNode>
        get() {
            if (childTreeNodes == null)
                throw SetChildTreeNodesNotCalledException()

            if (childTreeNodes!!.isEmpty())
                throw NoChildrenException()

            return childTreeNodes!!.filter { it.isSelected }
        }

    fun hasSelectedDescendants(): Boolean {
        if (childTreeNodes == null)
            throw SetChildTreeNodesNotCalledException()

        return childTreeNodes!!.any { it.isSelected || it.hasSelectedDescendants() }
    }

    override val treeNodeCollection by lazy { parent.treeNodeCollection }

    override val indentation by lazy { parent.indentation + 1 }

    fun select() {
        if (selected)
            throw SelectCalledTwiceException()

        if (!modelNode.isSelectable)
            throw NotSelectableSelectedException()

        onLongClick()
    }

    fun deselect() {
        if (!selected)
            throw NotSelectedException()

        onLongClick()
    }

    class SetChildTreeNodesNotCalledException : InitializationException("TreeNode.setChildTreeNodes() has not been called.")

    class SetChildTreeNodesCalledTwiceException : InitializationException("TreeNode.setChildTreeNodes() has already been called.")

    class NotSelectableSelectedException : IllegalStateException("A TreeNode cannot be selected if its ModelNode is not selectable.")

    class SelectableNotVisibleException : IllegalStateException("A TreeNode cannot be selectable if it isn't visible during action mode.")

    class EmptyExpandedException : IllegalStateException("A TreeNode cannot be expanded if it has no children.")

    class SelectAllException : UnsupportedOperationException("TreeViewAdapter.selectAll() can be called only if no nodes are selected.")

    class SelectedChildrenException : UnsupportedOperationException("A TreeNode cannot be collapsed if it has selected children.")

    class NoSuchNodeException : IllegalArgumentException("The given node is not a direct descendant of this TreeNode.")

    class InvisibleNodeException : UnsupportedOperationException("This operation is meaningless if the node is not visible.")

    class NoChildrenException : UnsupportedOperationException("Can't get selected children of a node that has no children.")

    class SelectCalledTwiceException : UnsupportedOperationException("Can't select a node that is already selected.")

    class NotSelectedException : UnsupportedOperationException("Can't deselect a node that is not selected.")

    class DescendantSelectedException : UnsupportedOperationException("Can't change a node's expansion state when it has selected descendants.")

    data class State(
            val isExpanded: Boolean,
            val isSelected: Boolean,
            val separatorVisibility: Boolean,
            val modelState: ModelState)

    override val id get() = modelNode.id
}
