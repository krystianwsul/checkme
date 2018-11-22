package com.krystianwsul.checkme.gui

import android.support.v7.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import com.krystianwsul.treeadapter.TreeViewAdapter


abstract class SelectionCallback(private val treeViewAdapterGetter: (() -> TreeViewAdapter)?) : ActionMode.Callback {

    private var selected = 0

    protected var actionMode: ActionMode? = null

    private var finishing = false

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        check(actionMode == null)

        actionMode = mode
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu) = false

    private fun (() -> TreeViewAdapter)?.update(action: () -> Unit) {
        if (this != null)
            this().updateDisplayedNodes(action)
        else
            action()
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        treeViewAdapterGetter.update {
            onMenuClick(item, TreeViewAdapter.Placeholder)

            check(!finishing)

            actionMode?.finish()
        }

        return true
    }

    override fun onDestroyActionMode(mode: ActionMode) {
        checkNotNull(actionMode)

        if (!finishing) {
            treeViewAdapterGetter.update {
                check(selected > 0)

                for (i in selected downTo 1) {
                    selected--

                    when (selected) {
                        0 -> {
                            actionMode = null

                            onLastRemoved(TreeViewAdapter.Placeholder)
                        }
                        1 -> onSecondToLastRemoved()
                        else -> onOtherRemoved()
                    }
                }

                unselect(TreeViewAdapter.Placeholder)
            }
        } else {
            actionMode = null

            unselect(TreeViewAdapter.Placeholder)
        }
    }

    fun setSelected(selected: Int, x: TreeViewAdapter.Placeholder) {
        if (selected > this.selected) {
            for (i in this.selected until selected)
                incrementSelected(x)
        } else if (selected < this.selected) {
            for (i in this.selected downTo selected + 1)
                decrementSelected(x)
        }
    }

    fun incrementSelected(x: TreeViewAdapter.Placeholder) {
        selected++

        when (selected) {
            1 -> {
                check(actionMode == null)
                onFirstAdded(x)
            }
            2 -> {
                checkNotNull(actionMode)
                onSecondAdded()
            }
            else -> {
                checkNotNull(actionMode)
                onOtherAdded()
            }
        }
    }

    fun decrementSelected(x: TreeViewAdapter.Placeholder) {
        check(selected > 0)
        checkNotNull(actionMode)

        selected--

        when (selected) {
            1 -> onSecondToLastRemoved()
            0 -> {
                check(!finishing)

                finishing = true
                actionMode!!.finish()
                finishing = false

                onLastRemoved(x)
            }
            else -> onOtherRemoved()
        }
    }

    val hasActionMode get() = actionMode != null

    protected abstract fun unselect(x: TreeViewAdapter.Placeholder)

    protected abstract fun onMenuClick(menuItem: MenuItem, x: TreeViewAdapter.Placeholder)

    protected abstract fun onFirstAdded(x: TreeViewAdapter.Placeholder)

    protected abstract fun onSecondAdded()

    protected abstract fun onOtherAdded()

    protected abstract fun onLastRemoved(x: TreeViewAdapter.Placeholder)

    protected abstract fun onSecondToLastRemoved()

    protected abstract fun onOtherRemoved()
}
