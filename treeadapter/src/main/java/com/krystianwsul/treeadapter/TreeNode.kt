package com.krystianwsul.treeadapter

import android.support.v7.widget.RecyclerView
import android.view.View
import java.util.*

class TreeNode(
        val modelNode: ModelNode,
        val parent: NodeContainer,
        private var expanded: Boolean,
        private var selected: Boolean) : Comparable<TreeNode>, NodeContainer {

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

    private val treeViewAdapter = treeNodeCollection.mTreeViewAdapter

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

                    val displayedSize = displayedSize()
                    expanded = false
                    treeNodeCollection.mTreeViewAdapter.notifyItemRangeRemoved(position + 1, displayedSize - 1)
                } else {
                    expanded = true
                    treeNodeCollection.mTreeViewAdapter.notifyItemRangeInserted(position + 1, displayedSize() - 1)
                }

                if (position > 0) {
                    treeNodeCollection.mTreeViewAdapter.notifyItemRangeChanged(position - 1, 2)
                } else {
                    treeNodeCollection.mTreeViewAdapter.notifyItemChanged(position)
                }
            }
        }

    val separatorVisibility: Boolean
        get() {
            if (!parent.isExpanded)
                throw InvisibleNodeException()

            val treeNodeCollection = treeNodeCollection

            val positionInCollection = treeNodeCollection.getPosition(this)
            check(positionInCollection >= 0)

            if (positionInCollection == treeNodeCollection.displayedSize() - 1)
                return false

            if (parent.getPosition(this) == parent.displayedSize() - 1)
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

    override fun update() = treeNodeCollection.let { it.mTreeViewAdapter.notifyItemChanged(it.getPosition(this)) }

    override fun updateRecursive() {
        update()

        parent.updateRecursive()
    }

    override fun isExpanded(): Boolean {
        check(!expanded || visibleSize() > 1)

        return expanded
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

            if (parent.selectedChildren.size == 0)
            // last in group
                parent.updateRecursive()
        }

        treeNodeCollection.let { it.mTreeViewAdapter.notifyItemChanged(it.getPosition(this)) }
    }

    private fun hasActionMode() = treeViewAdapter.hasActionMode()

    private fun incrementSelected() = treeViewAdapter.incrementSelected()

    private fun decrementSelected() = treeViewAdapter.decrementSelected()

    override fun displayedSize(): Int {
        if (childTreeNodes == null)
            throw SetChildTreeNodesNotCalledException()

        return if (!modelNode.isVisibleWhenEmpty && childTreeNodes!!.isEmpty() || !modelNode.isVisibleDuringActionMode && hasActionMode()) {
            0
        } else {
            1 + if (expanded) childTreeNodes!!.map { it.displayedSize() }.sum() else 0
        }
    }

    val displayedNodes: List<TreeNode>
        get() {
            if (childTreeNodes == null)
                throw SetChildTreeNodesNotCalledException()

            return if (!modelNode.isVisibleWhenEmpty && childTreeNodes!!.isEmpty() || !modelNode.isVisibleDuringActionMode && hasActionMode()) {
                listOf(this)
            } else {
                listOf(this) + childTreeNodes!!.flatMap { it.displayedNodes }
            }
        }

    private fun visibleSize(): Int {
        if (childTreeNodes == null)
            throw SetChildTreeNodesNotCalledException()

        return if (!modelNode.isVisibleWhenEmpty && childTreeNodes!!.isEmpty()) {
            0
        } else {
            1 + if (expanded) childTreeNodes!!.map { it.visibleSize() }.sum() else 0
        }
    }

    fun getNode(position: Int): TreeNode {
        var currPosition = position
        if (childTreeNodes == null)
            throw SetChildTreeNodesNotCalledException()

        check(currPosition >= 0)
        check(!childTreeNodes!!.isEmpty() || modelNode.isVisibleWhenEmpty)
        check(modelNode.isVisibleDuringActionMode || !hasActionMode())
        check(currPosition < displayedSize())

        if (currPosition == 0)
            return this

        check(expanded)

        currPosition--

        for (notDoneGroupTreeNode in childTreeNodes!!) {
            if (currPosition < notDoneGroupTreeNode.displayedSize())
                return notDoneGroupTreeNode.getNode(currPosition)

            currPosition -= notDoneGroupTreeNode.displayedSize()
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
            offset += childTreeNode.displayedSize()
        }

        return -1
    }

    fun unselect() {
        check(!selected || modelNode.isSelectable)

        val treeNodeCollection = treeNodeCollection

        if (selected) {
            check(modelNode.isSelectable)
            check(modelNode.isVisibleDuringActionMode)

            selected = false
            treeNodeCollection.mTreeViewAdapter.notifyItemChanged(treeNodeCollection.getPosition(this))
        }

        val selected = selectedNodes

        if (!selected.isEmpty()) {
            check(expanded)

            selected.forEach { it.unselect() }

            treeNodeCollection.mTreeViewAdapter.notifyItemChanged(treeNodeCollection.getPosition(this))
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

            treeNodeCollection.mTreeViewAdapter.notifyItemChanged(treeNodeCollection.getPosition(this))

            treeNodeCollection.mTreeViewAdapter.incrementSelected()
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

    override fun remove(childTreeNode: TreeNode) {
        if (childTreeNodes == null)
            throw SetChildTreeNodesNotCalledException()

        val treeNodeCollection = treeNodeCollection

        if (!childTreeNodes!!.contains(childTreeNode))
            throw NoSuchNodeException()

        val childDisplayedSize = childTreeNode.displayedSize()

        val oldParentPosition = treeNodeCollection.getPosition(this)

        val oldChildPosition = treeNodeCollection.getPosition(childTreeNode)

        val expanded = isExpanded
        val visible = visible()

        childTreeNodes!!.remove(childTreeNode)

        if (!visible)
            return

        check(oldParentPosition >= 0)

        if (expanded) {
            check(oldChildPosition >= 0)

            if (0 == childTreeNodes!!.map { it.displayedSize() }.sum()) {
                this.expanded = false

                if (oldParentPosition == 0) {
                    if (modelNode.isVisibleWhenEmpty) {
                        treeNodeCollection.mTreeViewAdapter.notifyItemChanged(oldParentPosition)
                        treeNodeCollection.mTreeViewAdapter.notifyItemRangeRemoved(oldParentPosition + 1, childDisplayedSize)
                    } else {
                        treeNodeCollection.mTreeViewAdapter.notifyItemRangeRemoved(oldParentPosition, 1 + childDisplayedSize)
                    }
                } else {
                    if (modelNode.isVisibleWhenEmpty) {
                        treeNodeCollection.mTreeViewAdapter.notifyItemRangeChanged(oldParentPosition - 1, 2)
                        treeNodeCollection.mTreeViewAdapter.notifyItemRangeRemoved(oldParentPosition + 1, childDisplayedSize)
                    } else {
                        treeNodeCollection.mTreeViewAdapter.notifyItemChanged(oldParentPosition - 1)
                        treeNodeCollection.mTreeViewAdapter.notifyItemRangeRemoved(oldParentPosition, 1 + childDisplayedSize)
                    }
                }
            } else {
                treeNodeCollection.mTreeViewAdapter.notifyItemChanged(oldParentPosition)
                treeNodeCollection.mTreeViewAdapter.notifyItemRangeRemoved(oldChildPosition, childDisplayedSize)

                treeNodeCollection.mTreeViewAdapter.notifyItemChanged(oldChildPosition - 1)

                if (oldParentPosition > 0)
                    treeNodeCollection.mTreeViewAdapter.notifyItemChanged(oldParentPosition - 1)
            }
        } else {
            if (0 == childTreeNodes!!.map { it.displayedSize() }.sum()) {
                if (oldParentPosition == 0) {
                    if (modelNode.isVisibleWhenEmpty) {
                        treeNodeCollection.mTreeViewAdapter.notifyItemChanged(oldParentPosition)
                    } else {
                        treeNodeCollection.mTreeViewAdapter.notifyItemRemoved(oldParentPosition)
                    }
                } else {
                    if (modelNode.isVisibleWhenEmpty) {
                        treeNodeCollection.mTreeViewAdapter.notifyItemChanged(oldParentPosition)
                    } else {
                        treeNodeCollection.mTreeViewAdapter.notifyItemChanged(oldParentPosition - 1)
                        treeNodeCollection.mTreeViewAdapter.notifyItemRemoved(oldParentPosition)
                    }
                }
            } else {
                treeNodeCollection.mTreeViewAdapter.notifyItemChanged(oldParentPosition)

                if (oldParentPosition > 0)
                    treeNodeCollection.mTreeViewAdapter.notifyItemChanged(oldParentPosition - 1)
            }
        }
    }

    fun removeAll() {
        if (childTreeNodes == null)
            throw SetChildTreeNodesNotCalledException()

        ArrayList(childTreeNodes!!).forEach { remove(it) }
    }

    override fun add(childTreeNode: TreeNode) {
        if (childTreeNodes == null)
            throw SetChildTreeNodesNotCalledException()

        val treeNodeCollection = treeNodeCollection

        if (expanded) {
            if (modelNode.isVisibleWhenEmpty) {
                val oldParentPosition = treeNodeCollection.getPosition(this)
                check(oldParentPosition >= 0)

                childTreeNodes!!.add(childTreeNode)

                childTreeNodes!!.sort()

                val newChildPosition = treeNodeCollection.getPosition(childTreeNode)
                check(newChildPosition >= 0)

                treeNodeCollection.mTreeViewAdapter.notifyItemChanged(oldParentPosition)
                treeNodeCollection.mTreeViewAdapter.notifyItemInserted(newChildPosition)

                val last = oldParentPosition + displayedSize() - 1 == newChildPosition

                if (last)
                    treeNodeCollection.mTreeViewAdapter.notifyItemChanged(newChildPosition - 1)

                if (oldParentPosition > 0)
                    treeNodeCollection.mTreeViewAdapter.notifyItemChanged(oldParentPosition - 1)
            } else {
                if (childTreeNodes!!.isEmpty()) {
                    childTreeNodes!!.add(childTreeNode)

                    childTreeNodes!!.sort()

                    val newParentPosition = treeNodeCollection.getPosition(this)

                    treeNodeCollection.mTreeViewAdapter.notifyItemInserted(newParentPosition + 1)

                    if (newParentPosition > 0)
                        treeNodeCollection.mTreeViewAdapter.notifyItemChanged(newParentPosition - 1)
                } else {
                    val oldParentPosition = treeNodeCollection.getPosition(this)
                    check(oldParentPosition >= 0)

                    childTreeNodes!!.add(childTreeNode)

                    childTreeNodes!!.sort()

                    val newChildPosition = treeNodeCollection.getPosition(childTreeNode)
                    check(newChildPosition >= 0)

                    treeNodeCollection.mTreeViewAdapter.notifyItemChanged(oldParentPosition)
                    treeNodeCollection.mTreeViewAdapter.notifyItemInserted(newChildPosition)

                    val last = oldParentPosition + displayedSize() - 1 == newChildPosition

                    if (last)
                        treeNodeCollection.mTreeViewAdapter.notifyItemChanged(newChildPosition - 1)

                    if (oldParentPosition > 0)
                        treeNodeCollection.mTreeViewAdapter.notifyItemChanged(oldParentPosition - 1)
                }
            }
        } else {
            childTreeNodes!!.add(childTreeNode)

            childTreeNodes!!.sort()

            if (modelNode.isVisibleDuringActionMode || !hasActionMode()) {
                val newParentPosition = treeNodeCollection.getPosition(this)
                check(newParentPosition >= 0)

                if (!modelNode.isVisibleWhenEmpty && childTreeNodes!!.size == 1) {
                    treeNodeCollection.mTreeViewAdapter.notifyItemInserted(newParentPosition)

                    if (newParentPosition > 0)
                        treeNodeCollection.mTreeViewAdapter.notifyItemChanged(newParentPosition - 1)
                } else {
                    treeNodeCollection.mTreeViewAdapter.notifyItemChanged(newParentPosition)
                }
            }
        }
    }

    override fun getSelectedChildren(): List<TreeNode> {
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

    fun onCreateActionMode() {
        if (childTreeNodes == null)
            throw SetChildTreeNodesNotCalledException()

        val treeNodeCollection = treeNodeCollection

        val position = treeNodeCollection.getPosition(this)
        check(position >= 0)

        if (modelNode.isVisibleDuringActionMode) {
            treeNodeCollection.mTreeViewAdapter.notifyItemChanged(position)

            if (expanded)
                childTreeNodes!!.forEach { it.onCreateActionMode() }
        } else {
            if (childTreeNodes!!.size > 0) {
                if (expanded) {
                    treeNodeCollection.mTreeViewAdapter.notifyItemRangeRemoved(position, visibleSize())
                } else {
                    treeNodeCollection.mTreeViewAdapter.notifyItemRemoved(position)
                }
            } else if (modelNode.isVisibleWhenEmpty) {
                treeNodeCollection.mTreeViewAdapter.notifyItemRemoved(position)
            }

            if (position > 0)
                treeNodeCollection.mTreeViewAdapter.notifyItemChanged(position - 1)
        }
    }

    fun onDestroyActionMode() {
        if (childTreeNodes == null)
            throw SetChildTreeNodesNotCalledException()

        val treeNodeCollection = treeNodeCollection

        val position = treeNodeCollection.getPosition(this)
        check(position >= 0)

        if (modelNode.isVisibleDuringActionMode) {
            treeNodeCollection.mTreeViewAdapter.notifyItemChanged(position)

            if (expanded)
                childTreeNodes!!.forEach { it.onDestroyActionMode() }
        } else {
            if (childTreeNodes!!.size > 0) {
                if (expanded) {
                    treeNodeCollection.mTreeViewAdapter.notifyItemRangeInserted(position, displayedSize())
                } else {
                    treeNodeCollection.mTreeViewAdapter.notifyItemInserted(position)
                }
            } else if (modelNode.isVisibleWhenEmpty) {
                treeNodeCollection.mTreeViewAdapter.notifyItemInserted(position)
            }

            if (position > 0)
                treeNodeCollection.mTreeViewAdapter.notifyItemChanged(position - 1)
        }
    }

    override fun getTreeNodeCollection() = parent.treeNodeCollection

    override fun getIndentation() = parent.indentation + 1

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
}
