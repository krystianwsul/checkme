package com.krystianwsul.treeadapter

import androidx.recyclerview.widget.RecyclerView
import java.util.*

class TreeNode<T : RecyclerView.ViewHolder>(
        val modelNode: ModelNode<T>,
        val parent: NodeContainer<T>,
        private var expanded: Boolean,
        private var selected: Boolean,
) : Comparable<TreeNode<T>>, NodeContainer<T> {

    override val isExpanded get() = expanded

    private lateinit var childTreeNodes: MutableList<TreeNode<T>>

    val itemViewType = modelNode.itemViewType

    fun onLongClick(viewHolder: RecyclerView.ViewHolder) {
        checkChildTreeNodesSet()

        if (!modelNode.isSelectable) return

        if (modelNode.isDraggable) {
            val startingDrag = modelNode.tryStartDrag(viewHolder)

            if (!startingDrag) selected = !selected

            modelNode.onBindViewHolder(viewHolder, startingDrag)

            if (!startingDrag) treeViewAdapter.updateDisplayedNodes { updateSelect(it, true) }
        } else {
            treeViewAdapter.updateDisplayedNodes(this::toggleSelected)
        }
    }

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
            checkChildTreeNodesSet()

            val selectedTreeNodes = ArrayList<TreeNode<T>>()

            if (selected) {
                check(modelNode.isSelectable)
                check(modelNode.isVisibleDuringActionMode)

                selectedTreeNodes += this
            }

            selectedTreeNodes += childTreeNodes.flatMap { it.selectedNodes }

            return selectedTreeNodes
        }

    val expandVisible: Boolean
        get() {
            checkChildTreeNodesSet()

            if (!visible()) throw InvisibleNodeException()

            if (childTreeNodes.none { it.canBeShown() }) return false

            return true
        }

    val state get() = State(isExpanded, isSelected, expandVisible, separatorVisible, modelNode.state)

    // hiding
    // showing
    fun onExpandClick() {
        checkChildTreeNodesSet()

        if (childTreeNodes.isEmpty()) throw EmptyExpandedException()

        treeViewAdapter.updateDisplayedNodes { placeholder ->
            expanded = if (expanded) { // collapsing
                childTreeNodes.forEach { it.deselectRecursive(placeholder) }

                false
            } else { // expanding
                if (selected) propagateSelection(true, placeholder)

                true
            }
        }
    }

    val separatorVisible: Boolean
        get() {
            if (!parent.isExpanded) throw InvisibleNodeException()

            val positionInCollection = treeNodeCollection.getPosition(this)
            check(positionInCollection >= 0)

            if (positionInCollection == treeNodeCollection.displayedNodes.size - 1) return false
            if (parent.getPosition(this) == parent.displayedNodes.size - 1) return parent.wantsSeparators

            val nextTreeNode = treeNodeCollection.getNode(positionInCollection + 1)
            return modelNode.isSeparatorVisibleWhenNotExpanded || nextTreeNode.wantsSeparators
        }

    override val wantsSeparators get() = displayedChildNodes.any { it.modelNode.showSeparatorWhenParentExpanded }

    val allChildren: List<TreeNode<T>> get() = childTreeNodes

    init {
        if (selected && !modelNode.isSelectable) throw NotSelectableSelectedException()

        if (modelNode.isSelectable && !modelNode.isVisibleDuringActionMode) throw SelectableNotVisibleException()
    }

    fun setChildTreeNodes(childTreeNodes: List<TreeNode<T>>) {
        if (this::childTreeNodes.isInitialized) throw SetChildTreeNodesCalledTwiceException()

        if (expanded && childTreeNodes.isEmpty()) throw EmptyExpandedException()

        this.childTreeNodes = childTreeNodes.sorted().toMutableList()
    }

    fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder) = modelNode.onBindViewHolder(viewHolder)

    override fun compareTo(other: TreeNode<T>) = modelNode.compareTo(other.modelNode)

    private fun toggleSelected(placeholder: TreeViewAdapter.Placeholder, recursive: Boolean = true) {
        checkChildTreeNodesSet()

        if (!modelNode.isSelectable) return

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
            childTreeNodes.filter { it.selected != selected }.forEach { it.toggleSelected(placeholder, false) }
    }

    private fun hasActionMode() = treeViewAdapter.hasActionMode

    private fun incrementSelected(placeholder: TreeViewAdapter.Placeholder) = treeViewAdapter.incrementSelected(placeholder)

    private fun decrementSelected(placeholder: TreeViewAdapter.Placeholder) = treeViewAdapter.decrementSelected(placeholder)

    fun getNode(position: Int): TreeNode<T> {
        checkChildTreeNodesSet()

        check(position in displayedNodes.indices)

        if (position == 0) return this

        check(expanded)

        var currPosition = position - 1

        for (notDoneGroupTreeNode in childTreeNodes) {
            val notDoneDisplayedSize = notDoneGroupTreeNode.displayedNodes.size

            if (currPosition < notDoneDisplayedSize) return notDoneGroupTreeNode.getNode(currPosition)

            currPosition -= notDoneDisplayedSize
        }

        throw IndexOutOfBoundsException()
    }

    override fun getPosition(treeNode: TreeNode<T>): Int {
        checkChildTreeNodesSet()

        if (treeNode === this) return 0
        if (!expanded) return -1

        var offset = 1
        for (childTreeNode in childTreeNodes) {
            val position = childTreeNode.getPosition(treeNode)

            if (position >= 0) return offset + position

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
        checkChildTreeNodesSet()

        if (modelNode.isSelectable) {
            check(modelNode.isVisibleDuringActionMode)

            selected = true

            treeViewAdapter.incrementSelected(placeholder)
        }

        if (expanded) {
            check(childTreeNodes.isNotEmpty())

            childTreeNodes.forEach { it.selectAll(placeholder) }
        }
    }

    override val displayedNodes: List<TreeNode<T>>
        get() {
            if (!canBeShown()) return listOf()

            return listOf(this) + displayedChildNodes
        }

    private val displayedChildNodes: List<TreeNode<T>>
        get() {
            check(canBeShown())

            return if (expanded)
                childTreeNodes.flatMap { it.displayedNodes }
            else
                listOf()
        }

    /**
     * todo: consider adding a cache that can be used when these values are known not to change, such as after
     * updateDisplayedNodes finishes running
     */
    fun canBeShown(): Boolean {
        checkChildTreeNodesSet()

        if (!modelNode.isVisibleDuringActionMode && hasActionMode()) return false

        if (treeViewAdapter.filterCriteria.hasQuery) {
            val matches = modelNode.parentHierarchyMatches(treeViewAdapter.filterCriteria)
                    || hasMatchingChild(treeViewAdapter.filterCriteria)

            if (!matches && modelNode.matches(treeViewAdapter.filterCriteria) == ModelNode.MatchResult.DOESNT_MATCH)
                return false
        }

        if (!modelNode.isVisibleWhenEmpty && childTreeNodes.none { it.canBeShown() }) return false

        return true
    }

    private fun matches(filterCriteria: Any) = modelNode.matches(filterCriteria)

    private fun hasMatchingChild(filterCriteria: Any): Boolean = childTreeNodes.any {
        it.matches(filterCriteria) == ModelNode.MatchResult.MATCHES || it.hasMatchingChild(filterCriteria)
    }

    private fun visible(): Boolean {
        checkChildTreeNodesSet()

        if (!canBeShown()) return false

        return if (parent is TreeNodeCollection<T>) {
            true
        } else {
            (parent as TreeNode).visible() && parent.isExpanded
        }
    }

    override fun remove(treeNode: TreeNode<T>, placeholder: TreeViewAdapter.Placeholder) {
        checkChildTreeNodesSet()

        if (!childTreeNodes.contains(treeNode))
            throw NoSuchNodeException()

        val visible = visible()

        childTreeNodes.remove(treeNode)

        if (!visible) return

        if (expanded && 0 == childTreeNodes.map { it.displayedNodes.size }.sum()) expanded = false
    }

    fun removeAll(placeholder: TreeViewAdapter.Placeholder) {
        checkChildTreeNodesSet()

        ArrayList(childTreeNodes).forEach { remove(it, placeholder) }
    }

    override fun add(treeNode: TreeNode<T>, placeholder: TreeViewAdapter.Placeholder) {
        checkChildTreeNodesSet()

        childTreeNodes.add(treeNode)
        childTreeNodes.sort()
    }

    override val selectedChildren: List<TreeNode<T>>
        get() {
            checkChildTreeNodesSet()

            if (childTreeNodes.isEmpty()) throw NoChildrenException()

            return childTreeNodes.filter { it.isSelected }
        }

    override val treeNodeCollection by lazy { parent.treeNodeCollection }

    override val indentation by lazy { parent.indentation + 1 }

    fun select(placeholder: TreeViewAdapter.Placeholder, recursive: Boolean = true) {
        if (selected) throw SelectCalledTwiceException()

        if (!modelNode.isSelectable) throw NotSelectableSelectedException()

        toggleSelected(placeholder, recursive)
    }

    fun deselect(placeholder: TreeViewAdapter.Placeholder, recursive: Boolean = true) {
        if (!selected) throw NotSelectedException()

        toggleSelected(placeholder, recursive)
    }

    private fun deselectRecursive(placeholder: TreeViewAdapter.Placeholder) {
        checkChildTreeNodesSet()

        if (selected) deselect(placeholder, false)
    }

    fun normalize() {
        modelNode.normalize()

        childTreeNodes.forEach { it.normalize() }
    }

    fun collapseAll() {
        childTreeNodes.forEach { it.collapseAll() }

        if (expanded) expanded = false
    }

    fun expandMatching(filterCriteria: Any, placeholder: TreeViewAdapter.Placeholder) {
        checkChildTreeNodesSet()

        if (hasMatchingChild(filterCriteria) && childTreeNodes.isNotEmpty()) expanded = true

        childTreeNodes.forEach { it.expandMatching(filterCriteria, placeholder) }
    }

    private fun checkChildTreeNodesSet() {
        if (!this::childTreeNodes.isInitialized) throw SetChildTreeNodesNotCalledException()
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
            val modelState: ModelState,
    )

    override val id get() = modelNode.id
}
