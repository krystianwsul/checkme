package com.krystianwsul.treeadapter

import android.os.Parcelable
import androidx.recyclerview.widget.RecyclerView
import kotlinx.parcelize.Parcelize
import java.util.*

class TreeNode<T : TreeHolder>(
        val modelNode: ModelNode<T>,
        val parent: NodeContainer<T>,
        private var selected: Boolean = false,
        private val initialExpansionState: ExpansionState? = null,
) : Comparable<TreeNode<T>>, NodeContainer<T> {

    lateinit var expansionState: ExpansionState
        private set

    override val isExpanded get() = expansionState.isExpanded

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

            return expandCanBeVisible
        }

    private val expandCanBeVisible: Boolean // if this node were visible, aka parent expanded
        get() {
            checkChildTreeNodesSet()

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

            val positionInCollection = treeNodeCollection.getPosition(this, PositionMode.DISPLAYED)
            check(positionInCollection >= 0)

            if (positionInCollection == treeNodeCollection.displayedNodes.size - 1) return false

            if (parent.getPosition(this, PositionMode.DISPLAYED) == parent.displayedNodes.size - 1)
                return parent.wantsSeparators

            val nextTreeNode = treeNodeCollection.getNode(positionInCollection + 1, PositionMode.DISPLAYED)
            return modelNode.isSeparatorVisibleWhenNotExpanded || nextTreeNode.wantsSeparators
        }

    override val wantsSeparators get() = displayedChildNodes.any { it.modelNode.showSeparatorWhenParentExpanded }

    val allChildren: List<TreeNode<T>> get() = childTreeNodes

    init {
        if (selected && !modelNode.isSelectable) throw NotSelectableSelectedException()

        if (modelNode.isSelectable && !modelNode.isVisibleDuringActionMode) throw SelectableNotVisibleException()
    }

    fun setChildTreeNodes(childTreeNodes: List<TreeNode<T>>) {
        // todo add delegate with final initialized state, move majority of function calls into it
        if (this::childTreeNodes.isInitialized) throw SetChildTreeNodesCalledTwiceException()

        expansionState = initialExpansionState?.takeIf { childTreeNodes.isNotEmpty() } ?: ExpansionState()

        this.childTreeNodes = childTreeNodes.sorted().toMutableList()
    }

    fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder) = modelNode.onBindViewHolder(viewHolder)

    fun onPayload(viewHolder: RecyclerView.ViewHolder, payloadSeparator: PayloadSeparator) =
            modelNode.onPayload(viewHolder, payloadSeparator)

    override fun compareTo(other: TreeNode<T>) = modelNode.compareTo(other.modelNode)

    private fun toggleSelected(placeholder: TreeViewAdapter.Placeholder, recursive: Boolean = true) {
        checkChildTreeNodesSet()

        if (!modelNode.isSelectable) return

        selected = !selected

        updateSelect(placeholder, recursive)
    }

    private fun updateSelect(placeholder: TreeViewAdapter.Placeholder, recursive: Boolean) {
        if (selected)
            incrementSelected(placeholder)
        else
            decrementSelected(placeholder)

        if (recursive && isExpanded) propagateSelection(selected, placeholder)

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

    override fun getPosition(treeNode: TreeNode<T>, positionMode: PositionMode): Int {
        checkChildTreeNodesSet()

        if (treeNode === this) return 0
        if (!isExpanded) return -1

        var offset = 1
        for (childTreeNode in positionMode.getDirectChildNodes(this)) {
            val position = childTreeNode.getPosition(treeNode, positionMode)

            if (position >= 0) return offset + position

            offset += positionMode.getRecursiveNodes(childTreeNode).size
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
            check(isExpanded)

            selected.forEach { it.unselect(placeholder) }
        }
    }

    fun select(placeholder: TreeViewAdapter.Placeholder) {
        checkChildTreeNodesSet()

        if (!modelNode.isSelectable) return

        check(modelNode.isVisibleDuringActionMode)

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

    private fun getLocker() = treeViewAdapter.locker?.getNodeLocker(this)

    /**
     * todo: consider adding a cache that can be used when these values are known not to change, such as after
     * updateDisplayedNodes finishes running
     */
    fun canBeShown(): Boolean {
        checkChildTreeNodesSet()

        if (!modelNode.isVisibleDuringActionMode && hasActionMode()) return false

        fun checkVisibleWhenEmpty() = modelNode.isVisibleWhenEmpty || childTreeNodes.any { it.canBeShown() }

        return when (val filterCriteria = treeViewAdapter.filterCriteria) {
            is FilterCriteria.Full -> {
                if (!matchesFilterParams(filterCriteria.filterParams)) return false

                when (modelNode.getMatchResult(filterCriteria.query)) {
                    ModelNode.MatchResult.ALWAYS_VISIBLE -> checkVisibleWhenEmpty()
                    ModelNode.MatchResult.MATCHES -> {
                        check(modelNode.isVisibleWhenEmpty)

                        true
                    }
                    ModelNode.MatchResult.DOESNT_MATCH -> {
                        check(modelNode.isVisibleWhenEmpty)

                        parentHierarchyMatchesQuery() || childHierarchyMatchesFilterCriteria(filterCriteria)
                    }
                }

            }
            is FilterCriteria.ExpandOnly, FilterCriteria.None -> checkVisibleWhenEmpty()
        }
    }

    private fun matchesQuery(query: String) =
            modelNode.getMatchResult(query) == ModelNode.MatchResult.MATCHES

    private fun matchesFilterParams(filterParams: FilterCriteria.Full.FilterParams) =
            modelNode.matchesFilterParams(filterParams)

    private fun matchesFilterCriteria(filterCriteria: FilterCriteria.Full) =
            matchesFilterParams(filterCriteria.filterParams) && matchesQuery(filterCriteria.query)

    private fun parentHierarchyMatchesQuery(): Boolean {
        return if (parent is TreeNode<T>) {
            parent.matchesQuery(treeViewAdapter.filterCriteria.query) || parent.parentHierarchyMatchesQuery()
        } else {
            false
        }
    }

    private fun childHierarchyMatchesFilterCriteria(filterCriteria: FilterCriteria.Full): Boolean = childTreeNodes.any {
        it.matchesFilterCriteria(filterCriteria) || it.childHierarchyMatchesFilterCriteria(filterCriteria)
    }

    private fun childHierarchyMatchesQuery(query: String): Boolean =
            childTreeNodes.any { it.matchesQuery(query) || it.childHierarchyMatchesQuery(query) }

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

        if (isExpanded && 0 == childTreeNodes.map { it.displayedNodes.size }.sum()) expansionState = ExpansionState()
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

    private fun deselectRecursive(placeholder: TreeViewAdapter.Placeholder) {
        checkChildTreeNodesSet()

        if (selected) toggleSelected(placeholder, false)
    }

    fun normalize() {
        modelNode.normalize()

        childTreeNodes.forEach { it.normalize() }
    }

    fun resetExpansion(onlyProgrammatic: Boolean, placeholder: TreeViewAdapter.Placeholder) {
        childTreeNodes.forEach { it.resetExpansion(onlyProgrammatic, placeholder) }

        if (onlyProgrammatic)
            expansionState.programmatic = false
        else
            expansionState = ExpansionState()
    }

    fun expandMatching(query: String, force: Boolean) {
        checkChildTreeNodesSet()

        /**
         * this used to check visible() and expandVisible, but I'm relaxing the condition so that No Reminders doesn't
         * get expanded, but if you do expand it manually, it expands all the way down to the match.
         */
        if (!expandCanBeVisible) return
        if (childTreeNodes.none { canBeShown() }) return

        if ((modelNode.expandOnMatch || force) && childHierarchyMatchesQuery(query)) expansionState.programmatic = true

        childTreeNodes.forEach { it.expandMatching(query, false) }
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

    class SetChildTreeNodesNotCalledException : InitializationException("TreeNode.setChildTreeNodes() has not been called.")

    class SetChildTreeNodesCalledTwiceException : InitializationException("TreeNode.setChildTreeNodes() has already been called.")

    class NotSelectableSelectedException : IllegalStateException("A TreeNode cannot be selected if its ModelNode is not selectable.")

    class SelectableNotVisibleException : IllegalStateException("A TreeNode cannot be selectable if it isn't visible during action mode.")

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
    class ExpansionState(var programmatic: Boolean = false, var user: Boolean? = null) : Parcelable {

        val isExpanded get() = user ?: programmatic
    }
}
