package com.krystianwsul.checkme.gui.instances.tree

import com.jakewharton.rxbinding3.view.clicks
import com.jakewharton.rxbinding3.view.longClicks
import com.krystianwsul.checkme.gui.instances.tree.checkable.CheckBoxState
import com.krystianwsul.checkme.gui.instances.tree.checkable.CheckableModelNode
import com.krystianwsul.treeadapter.TreeModelAdapter
import com.krystianwsul.treeadapter.TreeNodeCollection
import io.reactivex.Observable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.merge

abstract class GroupHolderAdapter : TreeModelAdapter<NodeHolder> {

    protected abstract val treeNodeCollection: TreeNodeCollection<NodeHolder>

    final override fun onViewAttachedToWindow(holder: NodeHolder) {
        holder.apply {
            fun Observable<*>.mapNodes() = map { adapterPosition }.filter { it >= 0 }.map {
                treeNodeCollection.getNode(it).let {
                    Pair(it, it.modelNode as GroupHolderNode)
                }
            }

            itemView.clicks()
                    .mapNodes()
                    .subscribe { (treeNode, _) -> treeNode.onClick(this) }
                    .addTo(compositeDisposable)

            itemView.longClicks { true }
                    .mapNodes()
                    .subscribe { (_, groupHolderNode) -> groupHolderNode.onLongClick(holder) }
                    .addTo(compositeDisposable)

            rowExpand.clicks()
                    .mapNodes()
                    .subscribe { (treeNode, _) -> treeNode.onExpandClick() }
                    .addTo(compositeDisposable)

            listOf(
                    rowCheckBoxFrame.clicks().doOnNext { rowCheckBox.toggle() },
                    rowCheckBox.clicks()
            ).merge()
                    .mapNodes()
                    .subscribe { (_, groupHolderNode) -> // todo delegate
                        ((groupHolderNode as? CheckableModelNode<*>)?.checkBoxState as? CheckBoxState.Visible)?.listener?.invoke()
                    }
                    .addTo(compositeDisposable)
        }
    }

    final override fun onViewDetachedFromWindow(holder: NodeHolder) = holder.compositeDisposable.clear()
}