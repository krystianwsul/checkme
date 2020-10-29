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

    fun onLongClick() = treeViewAdapter.updateDisplayedNodes(this::toggleSelected)

    fun onClick(holder: T) {
        if (hasActionMode()) {
            treeViewAdapter.updateDisplayedNodes(this::toggleSelected)
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

                selectedTreeNodes += this
            }

            selectedTreeNodes += childTreeNodes!!.flatMap { it.selectedNodes }

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

        treeViewAdapter.updateDisplayedNodes { placeholder ->
            expanded = if (expanded) { // collapsing
                childTreeNodes!!.forEach { it.deselectRecursive(placeholder) }

                false
            } else { // expanding
                if (selected) propagateSelection(true, placeholder)

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

            if (positionInCollection == treeNodeCollection.displayedNodes.size - 1)
                return false

            if (parent.getPosition(this) == parent.displayedNodes.size - 1)
                return true

            val nextTreeNode = treeNodeCollection.getNode(positionInCollection + 1)

            return nextTreeNode.isExpanded || modelNode.isSeparatorVisibleWhenNotExpanded
        }

    val allChildren: List<TreeNode<T>>
        get() = childTreeNodes
                ?: throw SetChildTreeNodesNotCalledException()

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

    private fun toggleSelected(placeholder: TreeViewAdapter.Placeholder, recursive: Boolean = true) {
        if (childTreeNodes == null)
            throw SetChildTreeNodesNotCalledException()

        if (!modelNode.isSelectable)
            return

        selected = !selected

        updateSelect(placeholder, recursive)
    }

    private fun updateSelect(placeholder: TreeViewAdapter.Placeholder, recursive: Boolean) {
        if (selected) {
            incrementSelected(placeholder)
        } else {
            decrementSelected(placeholder)
        }

        if (recursive && expanded)
            propagateSelection(selected, placeholder)

        if (recursive && !selected && modelNode.deselectParent) {
            (parent as TreeNode).takeIf { it.selected }?.toggleSelected(placeholder, false)
        }
    }

    private fun propagateSelection(selected: Boolean, placeholder: TreeViewAdapter.Placeholder) {
        if (modelNode.toggleDescendants)
            childTreeNodes!!.filter { it.selected != selected }.forEach { it.toggleSelected(placeholder, false) }
    }

    fun onLongClickSelect(viewHolder: RecyclerView.ViewHolder, startingDrag: Boolean) {
        if (!modelNode.isSelectable)
            return

        selected = !selected

        modelNode.onBindViewHolder(viewHolder, startingDrag)

        treeViewAdapter.updateDisplayedNodes { updateSelect(it, true) }
    }

    private fun hasActionMode() = treeViewAdapter.hasActionMode

    private fun incrementSelected(placeholder: TreeViewAdapter.Placeholder) = treeViewAdapter.incrementSelected(placeholder)

    private fun decrementSelected(placeholder: TreeViewAdapter.Placeholder) = treeViewAdapter.decrementSelected(placeholder)

    fun getNode(position: Int): TreeNode<T> {
        var currPosition = position
        if (childTreeNodes == null)
            throw SetChildTreeNodesNotCalledException()

        check(currPosition >= 0)
        check(currPosition < displayedNodes.size)

        if (currPosition == 0)
            return this

        check(expanded)

        currPosition--

        for (notDoneGroupTreeNode in childTreeNodes!!) {
            val notDoneDisplayedSize = notDoneGroupTreeNode.displayedNodes.size

            if (currPosition < notDoneDisplayedSize)
                return notDoneGroupTreeNode.getNode(currPosition)

            currPosition -= notDoneDisplayedSize
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
            offset += childTreeNode.displayedNodes.size
        }

        return -1
    }

    fun unselect(placeholder: TreeViewAdapter.Placeholder) {
        check(!selected || modelNode.isSelectable)

        if (selected) {
            check(modelNode.isSelectable)
            check(modelNode.isVisibleDuringActionMode)

            selected = false
        }

        val selected = selectedNodes

        if (selected.isNotEmpty()) {
            check(expanded)

            selected.forEach { it.unselect(placeholder) }
        }
    }

    fun selectAll(placeholder: TreeViewAdapter.Placeholder) {
        if (childTreeNodes == null)
            throw SetChildTreeNodesNotCalledException()

        if (modelNode.isSelectable) {
            check(modelNode.isVisibleDuringActionMode)

            selected = true

            treeViewAdapter.incrementSelected(placeholder)
        }

        if (expanded) {
            check(childTreeNodes!!.isNotEmpty())

            childTreeNodes!!.forEach { it.selectAll(placeholder) }
        }
    }

    override val displayedNodes: List<TreeNode<T>>
        get() {
            if (!canBeShown())
                return listOf()

            return if (expanded)
                listOf(this) + childTreeNodes!!.flatMap { it.displayedNodes }
            else
                listOf(this)
        }

    fun canBeShown(): Boolean {
        if (childTreeNodes == null) throw SetChildTreeNodesNotCalledException()

        if (!modelNode.isVisibleDuringActionMode && hasActionMode()) return false

        if (!modelNode.filter(treeViewAdapter.filterCriteria)) return false

        if (!modelNode.isVisibleWhenEmpty && childTreeNodes!!.none { it.canBeShown() }) return false

        return true
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

    override fun remove(treeNode: TreeNode<T>, placeholder: TreeViewAdapter.Placeholder) {
        if (childTreeNodes == null)
            throw SetChildTreeNodesNotCalledException()

        if (!childTreeNodes!!.contains(treeNode))
            throw NoSuchNodeException()

        val visible = visible()

        childTreeNodes!!.remove(treeNode)

        if (!visible)
            return

        if (expanded) {
            if (0 == childTreeNodes!!.map { it.displayedNodes.size }.sum())
                expanded = false
        }
    }

    fun removeAll(placeholder: TreeViewAdapter.Placeholder) {
        if (childTreeNodes == null)
            throw SetChildTreeNodesNotCalledException()

        ArrayList(childTreeNodes!!).forEach { remove(it, placeholder) }
    }

    override fun add(treeNode: TreeNode<T>, placeholder: TreeViewAdapter.Placeholder) {
        if (childTreeNodes == null)
            throw SetChildTreeNodesNotCalledException()

        childTreeNodes!! += treeNode
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

    fun select(placeholder: TreeViewAdapter.Placeholder, recursive: Boolean = true) {
        if (selected)
            throw SelectCalledTwiceException()

        if (!modelNode.isSelectable)
            throw NotSelectableSelectedException()

        toggleSelected(placeholder, recursive)
    }

    fun deselect(placeholder: TreeViewAdapter.Placeholder, recursive: Boolean = true) {
        if (!selected)
            throw NotSelectedException()

        toggleSelected(placeholder, recursive)
    }

    private fun deselectRecursive(placeholder: TreeViewAdapter.Placeholder) {
        childTreeNodes!!.forEach { it.deselectRecursive(placeholder) }

        if (selected)
            deselect(placeholder, false)
    }

    fun normalize() {
        modelNode.normalize()

        childTreeNodes!!.forEach { it.normalize() }
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
