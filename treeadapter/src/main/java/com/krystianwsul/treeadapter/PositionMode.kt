package com.krystianwsul.treeadapter

enum class PositionMode {

    COMPAT {

        override fun <T : TreeHolder> getDirectChildNodes(treeNodeCollection: TreeNodeCollection<T>) =
                treeNodeCollection.nodes

        override fun <T : TreeHolder> getRecursiveNodes(treeNodeCollection: TreeNodeCollection<T>) =
                treeNodeCollection.displayedNodes

        override fun <T : TreeHolder> getDirectChildNodes(treeNode: TreeNode<T>) = treeNode.allChildren

        override fun <T : TreeHolder> getRecursiveNodes(treeNode: TreeNode<T>) = treeNode.displayedNodes
    },
    DISPLAYED {

        override fun <T : TreeHolder> getDirectChildNodes(treeNodeCollection: TreeNodeCollection<T>) =
                treeNodeCollection.nodes.filter { it.canBeShown() }

        override fun <T : TreeHolder> getRecursiveNodes(treeNodeCollection: TreeNodeCollection<T>) =
                treeNodeCollection.displayedNodes

        override fun <T : TreeHolder> getDirectChildNodes(treeNode: TreeNode<T>) =
                treeNode.allChildren.filter { it.canBeShown() }

        override fun <T : TreeHolder> getRecursiveNodes(treeNode: TreeNode<T>) = treeNode.displayedNodes
    },
    ALL {

        override fun <T : TreeHolder> getDirectChildNodes(treeNodeCollection: TreeNodeCollection<T>) =
                throw NotImplementedError()

        override fun <T : TreeHolder> getRecursiveNodes(treeNodeCollection: TreeNodeCollection<T>) =
                throw NotImplementedError()

        override fun <T : TreeHolder> getDirectChildNodes(treeNode: TreeNode<T>) = throw NotImplementedError()
        override fun <T : TreeHolder> getRecursiveNodes(treeNode: TreeNode<T>) = throw NotImplementedError()
    };

    abstract fun <T : TreeHolder> getDirectChildNodes(treeNodeCollection: TreeNodeCollection<T>): List<TreeNode<T>>
    abstract fun <T : TreeHolder> getRecursiveNodes(treeNodeCollection: TreeNodeCollection<T>): List<TreeNode<T>>

    abstract fun <T : TreeHolder> getDirectChildNodes(treeNode: TreeNode<T>): List<TreeNode<T>>
    abstract fun <T : TreeHolder> getRecursiveNodes(treeNode: TreeNode<T>): List<TreeNode<T>>
}