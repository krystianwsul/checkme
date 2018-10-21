package com.krystianwsul.checkme.gui

import android.support.v7.view.ActionMode
import android.view.Menu
import android.view.MenuItem



abstract class SelectionCallback : ActionMode.Callback {

    private var mSelected = 0

    protected var actionMode: ActionMode? = null

    private var mFinishing = false

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        actionMode = mode
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu) = false

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        onMenuClick(item)

        check(!mFinishing)

        actionMode?.finish()

        return true
    }

    override fun onDestroyActionMode(mode: ActionMode) {
        check(actionMode != null)

        if (!mFinishing) {
            check(mSelected > 0)

            for (i in mSelected downTo 1) {
                mSelected--

                when (mSelected) {
                    1 -> onSecondToLastRemoved()
                    0 -> onLastRemoved { actionMode = null }
                    else -> onOtherRemoved()
                }
            }
        } else {
            actionMode = null
        }

        unselect()
    }

    fun setSelected(selected: Int) {
        if (selected > mSelected) {
            for (i in mSelected until selected)
                incrementSelected()
        } else if (selected < mSelected) {
            for (i in mSelected downTo selected + 1)
                decrementSelected()
        }
    }

    fun incrementSelected() {
        mSelected++

        when (mSelected) {
            1 -> {
                check(actionMode == null)
                onFirstAdded()
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

    fun decrementSelected() {
        check(mSelected > 0)
        check(actionMode != null)

        mSelected--

        when (mSelected) {
            1 -> onSecondToLastRemoved()
            0 -> {
                check(!mFinishing)

                onLastRemoved {
                    mFinishing = true
                    actionMode!!.finish()
                    mFinishing = false
                }
            }
            else -> onOtherRemoved()
        }
    }

    val hasActionMode get() = actionMode != null

    protected abstract fun unselect()

    protected abstract fun onMenuClick(menuItem: MenuItem)

    protected abstract fun onFirstAdded()

    protected abstract fun onSecondAdded()

    protected abstract fun onOtherAdded()

    protected abstract fun onLastRemoved(action: () -> Unit)

    protected abstract fun onSecondToLastRemoved()

    protected abstract fun onOtherRemoved()
}
