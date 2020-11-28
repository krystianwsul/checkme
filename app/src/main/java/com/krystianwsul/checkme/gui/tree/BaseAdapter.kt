package com.krystianwsul.checkme.gui.tree

import android.view.ViewGroup
import com.jakewharton.rxbinding3.view.clicks
import com.jakewharton.rxbinding3.view.longClicks
import com.krystianwsul.checkme.gui.tree.checkable.CheckBoxState
import com.krystianwsul.checkme.gui.tree.checkable.CheckableModelNode
import com.krystianwsul.treeadapter.TreeModelAdapter
import com.krystianwsul.treeadapter.TreeNodeCollection
import io.reactivex.Observable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.merge

abstract class BaseAdapter : TreeModelAdapter<BaseHolder> {

    protected abstract val treeNodeCollection: TreeNodeCollection<BaseHolder>

    final override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseHolder {
        val nodeType = NodeType.values()[viewType]

        return nodeType.onCreateViewHolder(parent)
    }

    final override fun onViewAttachedToWindow(holder: BaseHolder) {
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

            /* todo delegate consider moving this into the other classes.  Maybe publish observables in the holder,
            subscribe in the delegate, and add a disposable to the holder that gets cleared when something binds to it?
             */
            listOf(
                    rowCheckBoxFrame.clicks().doOnNext { rowCheckBox.toggle() },
                    rowCheckBox.clicks()
            ).merge()
                    .mapNodes()
                    .subscribe { (_, groupHolderNode) ->
                        ((groupHolderNode as? CheckableModelNode<*>)?.checkBoxState as? CheckBoxState.Visible)?.listener?.invoke()
                    }
                    .addTo(compositeDisposable)
        }
    }

    final override fun onViewDetachedFromWindow(holder: BaseHolder) = holder.compositeDisposable.clear()
}