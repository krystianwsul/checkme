package com.krystianwsul.checkme.gui

import android.support.v7.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import com.krystianwsul.treeadapter.TreeViewAdapter


abstract class SelectionCallback(private val treeViewAdapterGetter: (() -> TreeViewAdapter)?) : ActionMode.Callback {

    private var mSelected = 0

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
        check(actionMode != null)

        if (!finishing) {
            treeViewAdapterGetter.update {
                check(mSelected > 0)

                for (i in mSelected downTo 1) {
                    mSelected--

                    when (mSelected) {
                        0 -> onLastRemoved(TreeViewAdapter.Placeholder) { actionMode = null }
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
        if (selected > mSelected) {
            for (i in mSelected until selected)
                incrementSelected(x)
        } else if (selected < mSelected) {
            for (i in mSelected downTo selected + 1)
                decrementSelected(x)
        }
    }

    fun incrementSelected(x: TreeViewAdapter.Placeholder) {
        mSelected++

        when (mSelected) {
            1 -> {
                check(actionMode == null)
                onFirstAdded(x)
            }
            2 -> {
                check(actionMode != null)
                onSecondAdded()
            }
            else -> {
                check(actionMode != null)
                onOtherAdded()
            }
        }
    }

    fun decrementSelected(x: TreeViewAdapter.Placeholder) {
        check(mSelected > 0)
        check(actionMode != null)

        mSelected--

        when (mSelected) {
            1 -> onSecondToLastRemoved()
            0 -> {
                check(!finishing)

                onLastRemoved(x) {
                    finishing = true
                    actionMode!!.finish()
                    finishing = false
                }
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

    protected abstract fun onLastRemoved(x: TreeViewAdapter.Placeholder, action: () -> Unit) // todo remove action

    protected abstract fun onSecondToLastRemoved()

    protected abstract fun onOtherRemoved()
}
