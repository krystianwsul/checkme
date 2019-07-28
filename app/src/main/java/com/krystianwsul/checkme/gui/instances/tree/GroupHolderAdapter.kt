package com.krystianwsul.checkme.gui.instances.tree

import com.jakewharton.rxbinding3.view.clicks
import com.jakewharton.rxbinding3.view.longClicks
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.treeadapter.TreeModelAdapter
import com.krystianwsul.treeadapter.TreeNode
import com.krystianwsul.treeadapter.TreeNodeCollection
import io.reactivex.rxkotlin.addTo

abstract class GroupHolderAdapter : TreeModelAdapter<NodeHolder> {

    protected abstract val treeNodeCollection: TreeNodeCollection<NodeHolder>

    private fun checkStale() {
        if (treeNodeCollection.stale) {
            if (MyCrashlytics.enabled)
                MyCrashlytics.logException(GroupHolderNode.StaleTreeNodeException())
            else
                throw GroupHolderNode.StaleTreeNodeException()
        }
    }

    final override fun onViewAttachedToWindow(holder: NodeHolder) {
        checkStale()

        holder.apply {
            fun getTreeNode() = treeNodeCollection.getNode(adapterPosition)
            fun TreeNode<NodeHolder>.getGroupNode() = modelNode as GroupHolderNode

            itemView.clicks()
                    .subscribe {
                        checkStale()

                        val treeNode = getTreeNode()
                        val groupHolderNode = treeNode.getGroupNode()

                        val imageData = groupHolderNode.imageData

                        if (imageData != null)
                        else
                            treeNode.onClick(this)
                    }
                    .addTo(compositeDisposable)

            itemView.longClicks { true }
                    .subscribe {
                        getTreeNode().getGroupNode().apply {
                            checkStale()

                            onLongClick(holder)
                        }
                    }
                    .addTo(compositeDisposable)

            rowExpand.clicks()
                    .subscribe {
                        checkStale()

                        getTreeNode().onExpandClick()
                    }
                    .addTo(compositeDisposable)

            rowCheckBox.clicks()
                    .subscribe {
                        // todo rowCheckBox.setOnClickListener(null)

                        getTreeNode().getGroupNode().apply {
                            checkStale()

                            checkBoxOnClickListener()
                        }
                    }
                    .addTo(compositeDisposable)
        }
    }

    final override fun onViewDetachedFromWindow(holder: NodeHolder) = holder.compositeDisposable.clear()
}