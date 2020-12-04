package com.krystianwsul.checkme.gui.utils

import android.animation.Animator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.Menu
import android.view.MenuItem
import androidx.annotation.CallSuper
import androidx.appcompat.view.ActionMode
import androidx.core.content.ContextCompat
import com.google.android.material.bottomappbar.BottomAppBar
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.gui.tree.AbstractHolder
import com.krystianwsul.checkme.gui.widgets.MyBottomBar
import com.krystianwsul.checkme.utils.animateItems
import com.krystianwsul.treeadapter.ActionModeCallback
import com.krystianwsul.treeadapter.TreeViewAdapter
import kotlin.properties.Delegates.observable


abstract class SelectionCallback : ActionMode.Callback, ActionModeCallback {

    private var selected = 0

    var actionMode by observable<ActionMode?>(null) { _, _, newValue -> MyCrashlytics.log("actionMode = $newValue") }
        private set

    private var menuClickPlaceholder: TreeViewAdapter.Placeholder? = null
    private var removingLast = false

    protected abstract fun getTreeViewAdapter(): TreeViewAdapter<AbstractHolder>

    protected abstract val bottomBarData: Triple<MyBottomBar, Int, () -> Unit>

    private var initialBottomColor: Int? = null

    protected abstract val activity: Activity

    private var oldNavigationBarColor = -1

    protected open fun updateMenu() {
        val itemVisibilities = getItemVisibilities()

        itemVisibilities.forEach {
            actionMode!!.menu
                    .findItem(it.first)
                    ?.isVisible = it.second
        }

        bottomBarData.first.animateItems(itemVisibilities)
    }

    protected open fun getItemVisibilities() = listOf<Pair<Int, Boolean>>()

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        check(actionMode == null)

        actionMode = mode

        bottomBarData.first.let {
            if (initialBottomColor == null)
                initialBottomColor = it.backgroundTint!!.defaultColor

            val final = ContextCompat.getColor(it.context, R.color.actionModeBackground)
            it.animateBottom(final)

            it.animateReplaceMenu(bottomBarData.second) { updateMenu() }

            it.setOnMenuItemClickListener {
                onActionItemClicked(actionMode!!, it)

                true
            }

            it.navigationIcon = null
            it.setNavigationOnClickListener(null)
        }

        oldNavigationBarColor = activity.window.navigationBarColor

        activity.window.statusBarColor = Color.BLACK
        activity.window.navigationBarColor = Color.BLACK

        return true
    }

    private fun BottomAppBar.animateBottom(final: Int, endCallback: (() -> Unit)? = null) {
        val initial = backgroundTint!!.defaultColor

        ValueAnimator.ofArgb(initial, final).apply {
            duration = context.resources.getInteger(android.R.integer.config_longAnimTime).toLong()

            addUpdateListener { valueAnimator ->
                backgroundTint = ColorStateList.valueOf(valueAnimator.animatedValue as Int)
            }

            addListener(object : Animator.AnimatorListener {

                override fun onAnimationStart(animator: Animator) = Unit

                override fun onAnimationRepeat(animator: Animator) = Unit

                override fun onAnimationCancel(animator: Animator) = Unit

                override fun onAnimationEnd(animator: Animator) {
                    endCallback?.invoke()
                }
            })

            start()
        }
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu) = false

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        getTreeViewAdapter().updateDisplayedNodes {
            val close = onMenuClick(item.itemId, it)

            check(!removingLast)
            check(menuClickPlaceholder == null)

            if (close) {
                menuClickPlaceholder = it
                actionMode?.finish()
                menuClickPlaceholder = null
            }
        }

        return true
    }

    override fun onDestroyActionMode(mode: ActionMode) {
        checkNotNull(actionMode)

        when {
            removingLast -> {
                MyCrashlytics.log("actionMode = null from onDestroyActionMode")

                actionMode = null
            }
            menuClickPlaceholder != null -> countdown(menuClickPlaceholder!!)
            else -> getTreeViewAdapter().updateDisplayedNodes(::countdown)
        }

        bottomBarData.apply {
            first.animateBottom(initialBottomColor!!) {
                activity.window.statusBarColor = oldNavigationBarColor
                activity.window.navigationBarColor = oldNavigationBarColor
            }

            third.invoke()
        }
    }

    private fun countdown(placeholder: TreeViewAdapter.Placeholder) {
        check(selected > 0)

        for (i in selected downTo 1) {
            selected--

            when (selected) {
                0 -> {
                    MyCrashlytics.log("actionMode = null from countdown")
                    actionMode = null

                    onLastRemoved(placeholder)
                }
                1 -> onSecondToLastRemoved()
                else -> onOtherRemoved()
            }
        }

        unselect(placeholder)
    }

    fun setSelected(selected: Int, placeholder: TreeViewAdapter.Placeholder, initial: Boolean) {
        if (selected > this.selected) {
            var currentInitial = initial

            for (i in this.selected until selected) {
                incrementSelected(placeholder, currentInitial)
                currentInitial = false
            }
        } else if (selected < this.selected) {
            check(!initial)

            for (i in this.selected downTo selected + 1) decrementSelected(placeholder)
        }
    }

    override fun incrementSelected(placeholder: TreeViewAdapter.Placeholder, initial: Boolean) {
        selected++

        when (selected) {
            1 -> {
                check(actionMode == null)
                onFirstAdded(placeholder, initial)
            }
            2 -> {
                checkNotNull(actionMode)
                check(!initial)

                onSecondAdded()
            }
            else -> {
                checkNotNull(actionMode)
                check(!initial)

                onOtherAdded()
            }
        }

        updateTitle()
    }

    override fun decrementSelected(placeholder: TreeViewAdapter.Placeholder) {
        check(selected > 0)
        checkNotNull(actionMode)

        selected--

        when (selected) {
            1 -> {
                onSecondToLastRemoved()
                updateTitle()
            }
            0 -> {
                check(!removingLast)
                check(menuClickPlaceholder == null)

                removingLast = true
                actionMode!!.finish()
                removingLast = false

                onLastRemoved(placeholder)
            }
            else -> {
                onOtherRemoved()
                updateTitle()
            }
        }
    }

    private fun updateTitle() {
        actionMode!!.title = getTitleCount().toString()
    }

    open fun getTitleCount() = selected

    override val hasActionMode get() = actionMode != null

    protected abstract fun unselect(placeholder: TreeViewAdapter.Placeholder)

    protected abstract fun onMenuClick(itemId: Int, placeholder: TreeViewAdapter.Placeholder): Boolean

    @CallSuper
    protected open fun onFirstAdded(placeholder: TreeViewAdapter.Placeholder, initial: Boolean) = updateMenu()

    @CallSuper
    protected open fun onSecondAdded() = updateMenu()

    @CallSuper
    protected open fun onOtherAdded() = updateMenu()

    protected abstract fun onLastRemoved(placeholder: TreeViewAdapter.Placeholder)

    @CallSuper
    protected open fun onSecondToLastRemoved() = updateMenu()

    @CallSuper
    protected open fun onOtherRemoved() = updateMenu()
}
