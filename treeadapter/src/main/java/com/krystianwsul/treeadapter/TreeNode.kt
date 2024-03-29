package com.krystianwsul.treeadapter

import android.os.Parcelable
import androidx.recyclerview.widget.RecyclerView
import kotlinx.parcelize.Parcelize

class TreeNode<T : TreeHolder>(
    val modelNode: ModelNode<T>,
    var parent: NodeContainer<T>,
    private var selected: Boolean = false,
    private val initialExpansionState: ExpansionState? = null,
) : Comparable<TreeNode<T>>, NodeContainer<T> {

    override val treeNodeCollection by lazy { parent.treeNodeCollection }

    private lateinit var expansionState: ExpansionState

    fun getSaveExpansionState() = expansionState.takeIf { !it.isDefault }

    override val isExpanded get() = expansionState.isExpanded

    private lateinit var childTreeNodes: MutableList<TreeNode<T>>

    val itemViewType = modelNode.itemViewType

    fun onLongClick(viewHolder: RecyclerView.ViewHolder) {
        checkChildTreeNodesSet()

        if (!modelNode.isSelectable) return

        if (modelNode.tryStartDrag(viewHolder)) {
            modelNode.onBindViewHolder(viewHolder, true)
        } else {
            if (!selected) modelNode.forceSelected(viewHolder)

            treeViewAdapter.updateDisplayedNodes(this::toggleSelected)
        }
    }

    fun onClick(holder: T) {
        if (hasActionMode()) {
            if (!selected) modelNode.forceSelected(holder)

            treeViewAdapter.updateDisplayedNodes(this::toggleSelected)
        } else {
            modelNode.onClick(holder)
        }
    }

    val treeViewAdapter by lazy { treeNodeCollection.treeViewAdapter }

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
                check(modelNodeVisible)

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
            expansionState.user = if (isExpanded) { // collapsing
                childTreeNodes.forEach { it.setSelected(false, placeholder, false, true) }

                false
            } else { // expanding
                if (selected)
                    childTreeNodes.forEach { it.setSelected(true, placeholder, false, true) }

                true
            }
        }
    }

    val separatorVisible: Boolean
        get() {
            if (!parent.isExpanded) throw InvisibleNodeException()

            val positionInCollection = treeNodeCollection.getPosition(this, PositionMode.DISPLAYED)
            check(positionInCollection >= 0)

            if (positionInCollection == treeNodeCollection.displayedNodes.size - 1) return false

            if (parent.getPosition(this, PositionMode.DISPLAYED) == parent.displayedNodes.size - 1) {
                if (modelNode.inheritParentBottomSeparator && parent.showInheritableBottomSeparator) return true

                if (parent.wantsSeparators) return true
            }

            val nextTreeNode = treeNodeCollection.getNode(positionInCollection + 1, PositionMode.DISPLAYED)
            return modelNode.isSeparatorVisibleWhenNotExpanded || nextTreeNode.wantsSeparators
        }

    override val wantsSeparators get() = displayedChildNodes.any { it.modelNode.showSeparatorWhenParentExpanded }

    val allChildren: List<TreeNode<T>> get() = childTreeNodes

    override val showInheritableBottomSeparator: Boolean
        get() {
            if (this == parent.displayedDirectChildNodes.last()) return parent.wantsSeparators

            return false
        }

    init {
        if (selected && !modelNode.isSelectable) throw NotSelectableSelectedException()
    }

    fun setChildTreeNodes(childTreeNodes: List<TreeNode<T>>) {
        // todo add delegate with final initialized state, move majority of function calls into it
        if (this::childTreeNodes.isInitialized) throw SetChildTreeNodesCalledTwiceException()

        expansionState = initialExpansionState?.takeIf { childTreeNodes.isNotEmpty() } ?: ExpansionState()

        this.childTreeNodes = childTreeNodes.sorted().toMutableList()
    }

    fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder) = modelNode.onBindViewHolder(viewHolder)

    fun onPayload(viewHolder: RecyclerView.ViewHolder) = modelNode.onPayload(viewHolder)

    override fun compareTo(other: TreeNode<T>) = modelNode.compareTo(other.modelNode)

    private fun toggleSelected(placeholder: TreeViewAdapter.Placeholder) =
        setSelected(!selected, placeholder, true, true)

    private fun setSelected(
        selected: Boolean,
        placeholder: TreeViewAdapter.Placeholder,
        recurseParent: Boolean,
        recurseChildren: Boolean,
    ) {
        checkChildTreeNodesSet()

        if (!modelNode.isSelectable) return
        if (this.selected == selected) return

        this.selected = selected

        if (selected)
            incrementSelected(placeholder)
        else
            decrementSelected(placeholder)

        if (recurseChildren && isExpanded && modelNode.propagateSelection) {
            childTreeNodes.forEach { it.setSelected(selected, placeholder, false, true) }
        }

        if (recurseParent && !selected) {
            (parent as? TreeNode)?.takeIf {
                it.modelNode.propagateSelection
            }?.setSelected(false, placeholder, true, false)
        }
    }

    private fun hasActionMode() = treeViewAdapter.hasActionMode

    private fun incrementSelected(placeholder: TreeViewAdapter.Placeholder) = treeViewAdapter.incrementSelected(placeholder)

    private fun decrementSelected(placeholder: TreeViewAdapter.Placeholder) = treeViewAdapter.decrementSelected(placeholder)

    override fun getNode(position: Int, positionMode: PositionMode): TreeNode<T> {
        checkChildTreeNodesSet()

        check(position in positionMode.getRecursiveNodes(this).indices)

        if (position == 0) return this

        check(isExpanded)

        var currPosition = position - 1

        for (notDoneGroupTreeNode in positionMode.getDirectChildNodes(this)) {
            val notDoneDisplayedSize = positionMode.getRecursiveNodes(notDoneGroupTreeNode).size

            if (currPosition < notDoneDisplayedSize) return notDoneGroupTreeNode.getNode(currPosition, positionMode)

            currPosition -= notDoneDisplayedSize
        }

        throw IndexOutOfBoundsException()
    }

    override fun getPosition(positionMode: PositionMode, matcher: (TreeNode<T>) -> Boolean): Int {
        checkChildTreeNodesSet()

        if (matcher(this)) return 0
        if (!isExpanded) return -1

        var offset = 1
        for (childTreeNode in positionMode.getDirectChildNodes(this)) {
            val position = childTreeNode.getPosition(positionMode, matcher)

            if (position >= 0) return offset + position

            offset += positionMode.getRecursiveNodes(childTreeNode).size
        }

        return -1
    }

    fun unselect(placeholder: TreeViewAdapter.Placeholder) {
        check(!selected || modelNode.isSelectable)

        if (selected) {
            check(modelNode.isSelectable)
            check(modelNodeVisible)

            selected = false
        }

        val selected = selectedNodes

        if (selected.isNotEmpty()) {
            check(isExpanded)

            selected.forEach { it.unselect(placeholder) }
        }
    }

    fun select(placeholder: TreeViewAdapter.Placeholder) {
        checkChildTreeNodesSet()

        if (!modelNode.isSelectable) return

        check(modelNodeVisible)

        selected = true

        treeViewAdapter.incrementSelected(placeholder)
    }

    override val displayedNodes: List<TreeNode<T>>
        get() {
            val locker = getLocker()
            locker?.displayedNodes?.let { return it }

            val result = if (canBeShown()) listOf(this) + displayedChildNodes else listOf()

            locker?.displayedNodes = result
            return result
        }

    override val displayableNodes: List<TreeNode<T>>
        get() {
            if (!canBeShown()) return listOf()

            return listOf(this) + childTreeNodes.flatMap { it.displayableNodes }
        }

    override val displayedChildNodes: List<TreeNode<T>>
        get() {
            check(canBeShown())

            return if (isExpanded)
                childTreeNodes.flatMap { it.displayedNodes }
            else
                listOf()
        }

    override val displayedDirectChildNodes: List<TreeNode<T>>
        get() {
            check(visible())

            return childTreeNodes.filter { it.visible() }
        }

    private fun getLocker() = treeViewAdapter.locker?.getNodeLocker(this)

    private val modelNodeVisible get() = modelNode.isVisible(hasActionMode(), childTreeNodes.any { it.canBeShown() })

    /**
     * todo: consider adding a cache that can be used when these values are known not to change, such as after
     * updateDisplayedNodes finishes running
     */
    fun canBeShown(): Boolean {
        checkChildTreeNodesSet()

        return modelNodeVisible
    }

    private val matchesSearch = modelNode.matchesSearch

    private val childHierarchyMatchesSearch: Boolean get() = childTreeNodes.any { it.matchesSearch || it.childHierarchyMatchesSearch }

    fun visible(): Boolean {
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

        if (isExpanded && 0 == childTreeNodes.sumOf { it.displayedNodes.size }) expansionState = ExpansionState()
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

    override val indentation by lazy { parent.indentation + 1 }

    fun resetExpansion(onlyProgrammatic: Boolean, placeholder: TreeViewAdapter.Placeholder) {
        childTreeNodes.forEach { it.resetExpansion(onlyProgrammatic, placeholder) }

        if (onlyProgrammatic)
            expansionState.programmatic = false
        else
            expansionState = ExpansionState()
    }

    fun expandMatching() {
        checkChildTreeNodesSet()

        if (childHierarchyMatchesSearch) expansionState.programmatic = true

        childTreeNodes.forEach { it.expandMatching() }
    }

    private fun checkChildTreeNodesSet() {
        if (!this::childTreeNodes.isInitialized) throw SetChildTreeNodesNotCalledException()
    }

    override fun swapNodePositions(
        fromTreeNode: TreeNode<T>,
        toTreeNode: TreeNode<T>,
        placeholder: TreeViewAdapter.Placeholder,
    ) {
        check(treeViewAdapter.locker == null)

        val fromPosition = childTreeNodes.indexOf(fromTreeNode)
        check(fromPosition >= 0)

        val toPosition = childTreeNodes.indexOf(toTreeNode)
        check(toPosition >= 0)

        childTreeNodes.apply {
            removeAt(fromPosition)
            add(toPosition, fromTreeNode)
        }
    }

    override fun removeForSwap(fromTreeNode: TreeNode<T>, placeholder: TreeViewAdapter.Placeholder) {
        check(treeViewAdapter.locker == null)

        val fromPosition = childTreeNodes.indexOf(fromTreeNode)
        check(fromPosition >= 0)

        childTreeNodes.removeAt(fromPosition)
    }

    override fun addForSwap(fromTreeNode: TreeNode<T>, toTreeNode: TreeNode<T>, placeholder: TreeViewAdapter.Placeholder) {
        check(treeViewAdapter.locker == null)

        val toPosition = childTreeNodes.indexOf(toTreeNode)
        check(toPosition >= 0)

        fromTreeNode.parent = this
        childTreeNodes.add(toPosition, fromTreeNode)
    }

    class SetChildTreeNodesNotCalledException : InitializationException("TreeNode.setChildTreeNodes() has not been called.")

    class SetChildTreeNodesCalledTwiceException :
        InitializationException("TreeNode.setChildTreeNodes() has already been called.")

    class NotSelectableSelectedException :
        IllegalStateException("A TreeNode cannot be selected if its ModelNode is not selectable.")

    class EmptyExpandedException : IllegalStateException("A TreeNode cannot be expanded if it has no children.")

    class NoSuchNodeException : IllegalArgumentException("The given node is not a direct descendant of this TreeNode.")

    class InvisibleNodeException : UnsupportedOperationException("This operation is meaningless if the node is not visible.")

    class NoChildrenException : UnsupportedOperationException("Can't get selected children of a node that has no children.")

    data class State(
        val isExpanded: Boolean,
        val isSelected: Boolean,
        val expandVisible: Boolean,
        val separatorVisibility: Boolean,
        val modelState: ModelState,
    ) {

        fun getPayload(other: State) = if (isExpanded == other.isExpanded &&
            isSelected == other.isSelected &&
            expandVisible == other.expandVisible &&
            separatorVisibility != other.separatorVisibility &&
            modelState == other.modelState
        ) PayloadSeparator else null
    }

    object PayloadSeparator

    override val id get() = modelNode.id

    @Parcelize
    data class ExpansionState(var programmatic: Boolean = false, var user: Boolean? = null) : Parcelable {

        companion object {

            private val default = ExpansionState()
        }

        val isExpanded get() = user ?: programmatic

        val isDefault get() = this == default
    }
}
