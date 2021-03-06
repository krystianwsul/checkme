package com.krystianwsul.treeadapter

import androidx.recyclerview.widget.RecyclerView
import java.util.*

class TreeNode<T : RecyclerView.ViewHolder>(
        val modelNode: ModelNode<T>,
        val parent: NodeContainer<T>,
        private var expanded: Boolean,
        private var selected: Boolean
) : Comparable<TreeNode<T>>, NodeContainer<T> {

    override val isExpanded get() = expanded

    private var childTreeNodes: MutableList<TreeNode<T>>? = null

    val itemViewType = modelNode.itemViewType

    fun onLongClick() {
        treeViewAdapter.updateDisplayedNodes {
            toggleSelected(TreeViewAdapter.Placeholder)
        }
    }

    fun onClick(holder: T) {
        if (hasActionMode()) {
            treeViewAdapter.updateDisplayedNodes {
                toggleSelected(TreeViewAdapter.Placeholder)
            }
        } else {
            modelNode.onClick(holder)
        }
    }

    private val treeViewAdapter by lazy { treeNodeCollection.treeViewAdapter }

    val isSelected: Boolean
        get() {
            check(!selected || modelNode.isSelectable)

            return selected
        }

    val selectedNodes: List<TreeNode<T>>
        get() {
            if (childTreeNodes == null)
                throw SetChildTreeNodesNotCalledException()

            val selectedTreeNodes = ArrayList<TreeNode<T>>()

            if (selected) {
                check(modelNode.isSelectable)
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

            if (childTreeNodes!!.none { it.canBeShown() })
                return false

            return true
        }

    val state get() = State(isExpanded, isSelected, expandVisible, separatorVisible, modelNode.state)

    // hiding
    // showing
    fun onExpandClick() {
        if (childTreeNodes == null)
            throw SetChildTreeNodesNotCalledException()

        if (childTreeNodes!!.isEmpty())
            throw EmptyExpandedException()

        treeViewAdapter.updateDisplayedNodes {
            expanded = if (expanded) { // collapsing
                childTreeNodes!!.forEach { it.deselectRecursive(TreeViewAdapter.Placeholder) }

                false
            } else { // expanding
                if (selected)
                    propagateSelection(true, TreeViewAdapter.Placeholder)

                true
            }
        }
    }

    val separatorVisible: Boolean
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

    val allChildren: List<TreeNode<T>>
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

    fun setChildTreeNodes(childTreeNodes: List<TreeNode<T>>) {
        if (this.childTreeNodes != null)
            throw SetChildTreeNodesCalledTwiceException()

        if (expanded && childTreeNodes.isEmpty())
            throw EmptyExpandedException()

        this.childTreeNodes = ArrayList(childTreeNodes)

        this.childTreeNodes!!.sort()
    }

    fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder) = modelNode.onBindViewHolder(viewHolder)

    override fun compareTo(other: TreeNode<T>) = modelNode.compareTo(other.modelNode)

    private fun toggleSelected(x: TreeViewAdapter.Placeholder, recursive: Boolean = true) {
        if (childTreeNodes == null)
            throw SetChildTreeNodesNotCalledException()

        if (!modelNode.isSelectable)
            return

        selected = !selected

        updateSelect(x, recursive)
    }

    private fun updateSelect(x: TreeViewAdapter.Placeholder, recursive: Boolean) {
        if (selected) {
            incrementSelected(x)
        } else {
            decrementSelected(x)
        }

        if (recursive && expanded)
            propagateSelection(selected, x)

        if (recursive && !selected && modelNode.deselectParent) {
            (parent as TreeNode).takeIf { it.selected }?.toggleSelected(x, false)
        }
    }

    private fun propagateSelection(selected: Boolean, x: TreeViewAdapter.Placeholder) {
        if (modelNode.toggleDescendants)
            childTreeNodes!!.filter { it.selected != selected }.forEach { it.toggleSelected(x, false) }
    }

    fun onLongClickSelect(viewHolder: RecyclerView.ViewHolder, startingDrag: Boolean) {
        if (!modelNode.isSelectable)
            return

        selected = !selected

        modelNode.onBindViewHolder(viewHolder, startingDrag)

        treeViewAdapter.updateDisplayedNodes {
            updateSelect(TreeViewAdapter.Placeholder, true)
        }
    }

    private fun hasActionMode() = treeViewAdapter.hasActionMode()

    private fun incrementSelected(x: TreeViewAdapter.Placeholder) = treeViewAdapter.incrementSelected(x)

    private fun decrementSelected(x: TreeViewAdapter.Placeholder) = treeViewAdapter.decrementSelected(x)

    override val displayedSize: Int
        get() {
            if (childTreeNodes == null)
                throw SetChildTreeNodesNotCalledException()

            return if ((!modelNode.isVisibleWhenEmpty && childTreeNodes!!.isEmpty()) || (!modelNode.isVisibleDuringActionMode && hasActionMode()) || !modelNode.filter()) {
                0
            } else {
                1 + if (expanded) childTreeNodes!!.map { it.displayedSize }.sum() else 0
            }
        }

    val displayedNodes: List<TreeNode<T>>
        get() {
            if (childTreeNodes == null)
                throw SetChildTreeNodesNotCalledException()

            return if (!modelNode.isVisibleWhenEmpty && childTreeNodes!!.isEmpty() || !modelNode.isVisibleDuringActionMode && hasActionMode() || !modelNode.filter()) {
                listOf()
            } else {
                if (expanded)
                    listOf(this) + childTreeNodes!!.flatMap { it.displayedNodes }
                else
                    listOf(this)
            }
        }

    fun getNode(position: Int): TreeNode<T> {
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

    override fun getPosition(treeNode: TreeNode<T>): Int {
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

    fun unselect(x: TreeViewAdapter.Placeholder) {
        check(!selected || modelNode.isSelectable)

        if (selected) {
            check(modelNode.isSelectable)
            check(modelNode.isVisibleDuringActionMode)

            selected = false
        }

        val selected = selectedNodes

        if (selected.isNotEmpty()) {
            check(expanded)

            selected.forEach { it.unselect(x) }
        }
    }

    fun selectAll(x: TreeViewAdapter.Placeholder) {
        if (childTreeNodes == null)
            throw SetChildTreeNodesNotCalledException()

        if (modelNode.isSelectable) {
            check(modelNode.isVisibleDuringActionMode)

            selected = true

            treeViewAdapter.incrementSelected(x)
        }

        if (expanded) {
            check(childTreeNodes!!.isNotEmpty())

            childTreeNodes!!.forEach { it.selectAll(x) }
        }
    }

    fun canBeShown(): Boolean {
        if (childTreeNodes == null)
            throw SetChildTreeNodesNotCalledException()

        if (!modelNode.isVisibleDuringActionMode && hasActionMode())
            return false

        if (!modelNode.filter())
            return false

        return !(!modelNode.isVisibleWhenEmpty && childTreeNodes!!.isEmpty())
    }

    private fun visible(): Boolean {
        if (childTreeNodes == null)
            throw SetChildTreeNodesNotCalledException()

        if (!canBeShown())
            return false

        return if (parent is TreeNodeCollection<T>) {
            true
        } else {
            (parent as TreeNode).visible() && parent.isExpanded
        }
    }

    override fun remove(treeNode: TreeNode<T>, x: TreeViewAdapter.Placeholder) {
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

    fun removeAll(x: TreeViewAdapter.Placeholder) {
        if (childTreeNodes == null)
            throw SetChildTreeNodesNotCalledException()

        ArrayList(childTreeNodes!!).forEach { remove(it, x) }
    }

    override fun add(treeNode: TreeNode<T>, x: TreeViewAdapter.Placeholder) {
        if (childTreeNodes == null)
            throw SetChildTreeNodesNotCalledException()

        childTreeNodes!!.add(treeNode)
        childTreeNodes!!.sort()
    }

    override val selectedChildren: List<TreeNode<T>>
        get() {
            if (childTreeNodes == null)
                throw SetChildTreeNodesNotCalledException()

            if (childTreeNodes!!.isEmpty())
                throw NoChildrenException()

            return childTreeNodes!!.filter { it.isSelected }
        }

    override val treeNodeCollection by lazy { parent.treeNodeCollection }

    override val indentation by lazy { parent.indentation + 1 }

    fun select(x: TreeViewAdapter.Placeholder, recursive: Boolean = true) {
        if (selected)
            throw SelectCalledTwiceException()

        if (!modelNode.isSelectable)
            throw NotSelectableSelectedException()

        toggleSelected(x, recursive)
    }

    fun deselect(x: TreeViewAdapter.Placeholder, recursive: Boolean = true) {
        if (!selected)
            throw NotSelectedException()

        toggleSelected(x, recursive)
    }

    private fun deselectRecursive(x: TreeViewAdapter.Placeholder) {
        childTreeNodes!!.forEach { it.deselectRecursive(x) }

        if (selected)
            deselect(x, false)
    }

    class SetChildTreeNodesNotCalledException : InitializationException("TreeNode.setChildTreeNodes() has not been called.")

    class SetChildTreeNodesCalledTwiceException : InitializationException("TreeNode.setChildTreeNodes() has already been called.")

    class NotSelectableSelectedException : IllegalStateException("A TreeNode cannot be selected if its ModelNode is not selectable.")

    class SelectableNotVisibleException : IllegalStateException("A TreeNode cannot be selectable if it isn't visible during action mode.")

    class EmptyExpandedException : IllegalStateException("A TreeNode cannot be expanded if it has no children.")

    class NoSuchNodeException : IllegalArgumentException("The given node is not a direct descendant of this TreeNode.")

    class InvisibleNodeException : UnsupportedOperationException("This operation is meaningless if the node is not visible.")

    class NoChildrenException : UnsupportedOperationException("Can't get selected children of a node that has no children.")

    class SelectCalledTwiceException : UnsupportedOperationException("Can't select a node that is already selected.")

    class NotSelectedException : UnsupportedOperationException("Can't deselect a node that is not selected.")

    data class State(
            val isExpanded: Boolean,
            val isSelected: Boolean,
            val expandVisible: Boolean,
            val separatorVisibility: Boolean,
            val modelState: ModelState
    )

    override val id get() = modelNode.id
}
