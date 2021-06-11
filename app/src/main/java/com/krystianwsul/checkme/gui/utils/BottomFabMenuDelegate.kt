package com.krystianwsul.checkme.gui.utils

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.ColorRes
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.transition.TransitionManager
import com.google.android.material.color.MaterialColors
import com.google.android.material.transition.MaterialContainerTransform
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.databinding.BottomBinding
import com.krystianwsul.checkme.databinding.ItemFabMenuBinding
import kotlinx.parcelize.Parcelize

class BottomFabMenuDelegate(
    private val bottomBinding: BottomBinding,
    private val coordinatorLayout: CoordinatorLayout,
    private val activity: Activity,
    initialState: State?,
) {

    var state = initialState ?: State()
        private set

    val fabDelegate = object : FabDelegate {

        override fun show() {
            state.fabVisibleWhenNoMenu = true

            if (state.menuDelegate == null) bottomBinding.bottomFab.show()
        }

        override fun hide() {
            state.fabVisibleWhenNoMenu = false

            if (state.menuDelegate == null) bottomBinding.bottomFab.hide()
        }

        override fun setOnClickListener(listener: () -> Unit) {
            bottomBinding.bottomFab.setOnClickListener { listener() }
        }
    }

    init {
        bottomBinding.bottomFabScrim.setOnClickListener { closeMenu() }

        if (state.menuDelegate != null) {
            showMenuInternal(false)
        } else if (state.fabVisibleWhenNoMenu) {
            bottomBinding.bottomFab.isVisible = true
        }
    }

    private fun closeMenu() {
        checkNotNull(state.menuDelegate)
        state.menuDelegate = null

        bottomBinding.apply {
            if (state.fabVisibleWhenNoMenu) {
                val transition = buildContainerTransformation()

                transition.startView = bottomFabMenu
                transition.endView = bottomFab

                transition.addTarget(bottomFab)

                TransitionManager.beginDelayedTransition(coordinatorLayout, transition)

                bottomFab.visibility = View.VISIBLE
            } else {
                TransitionManager.beginDelayedTransition(coordinatorLayout)
            }

            bottomFabMenu.visibility = View.INVISIBLE
            bottomFabScrim.visibility = View.INVISIBLE

            setWindowColor(R.color.primaryDarkColor)
        }
    }

    private fun buildContainerTransformation() = MaterialContainerTransform().apply {
        containerColor = MaterialColors.getColor(bottomBinding.root, R.attr.colorSecondary)
        scrimColor = Color.TRANSPARENT
        duration = 300
        interpolator = FastOutSlowInInterpolator()
        fadeMode = MaterialContainerTransform.FADE_MODE_IN
    }

    fun showMenu(menuDelegate: MenuDelegate) {
        check(state.menuDelegate == null)
        state.menuDelegate = menuDelegate

        showMenuInternal(true)
    }

    private fun showMenuInternal(animate: Boolean) {
        checkNotNull(state.menuDelegate)

        bottomBinding.apply {
            bottomFabMenuList.removeAllViews()

            val items = state.menuDelegate!!.getItems()

            items.dropLast(1).forEach {
                ItemFabMenuBinding.inflate(
                    LayoutInflater.from(bottomFabMenu.context),
                    bottomFabMenuList,
                    true,
                )
                    .root
                    .apply {
                        text = it.getText(bottomFabMenu.context)

                        setOnClickListener { _ ->
                            it.onClick(activity)
                            closeMenu()
                        }
                    }
            }

            items.last().let {
                bottomBinding.bottomFabMenuButton.apply {
                    text = it.getText(bottomFabMenu.context)

                    setOnClickListener { _ ->
                        it.onClick(activity)
                        closeMenu()
                    }
                }
            }

            if (animate) {
                val transition = buildContainerTransformation()

                transition.startView = bottomFab
                transition.endView = bottomFabMenu

                transition.addTarget(bottomFabMenu)

                TransitionManager.beginDelayedTransition(activity.findViewById(android.R.id.content), transition)
            }

            bottomFabMenu.visibility = View.VISIBLE
            bottomFabScrim.visibility = View.VISIBLE

            setWindowColor(R.color.statusBarScrim)

            bottomFab.visibility = View.INVISIBLE
        }
    }

    private fun setWindowColor(@ColorRes colorId: Int) {
        val color = ContextCompat.getColor(activity, colorId)

        activity.window.apply {
            activity.window.statusBarColor = color
            activity.window.navigationBarColor = color
        }
    }

    interface MenuDelegate : Parcelable {

        fun getItems(): List<Item>

        interface Item {

            fun getText(context: Context): String

            fun onClick(activity: Activity)
        }
    }

    interface FabDelegate {

        fun show()

        fun hide()

        fun setOnClickListener(listener: () -> Unit)
    }

    @Parcelize
    data class State(
        var fabVisibleWhenNoMenu: Boolean = false,
        var menuDelegate: MenuDelegate? = null,
    ) : Parcelable
}