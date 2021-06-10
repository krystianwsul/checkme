package com.krystianwsul.checkme.gui.utils

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.transition.TransitionManager
import com.google.android.material.color.MaterialColors
import com.google.android.material.transition.MaterialContainerTransform
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.databinding.BottomBinding
import com.krystianwsul.checkme.databinding.ItemFabMenuBinding

class BottomFabMenuDelegate(
    private val bottomBinding: BottomBinding,
    private val coordinatorLayout: CoordinatorLayout,
    private val activity: Activity,
    initialMenuDelegate: MenuDelegate?,
) {

    private var menuDelegate: MenuDelegate? = null

    val fabDelegate = object : FabDelegate {

        override fun show() {
            bottomBinding.bottomFab.show() // todo fab
        }

        override fun hide() {
            bottomBinding.bottomFab.hide() // todo fab
        }

        override fun setOnClickListener(listener: () -> Unit) {
            bottomBinding.bottomFab.setOnClickListener { listener() } // todo fab
        }
    }

    init {
        bottomBinding.bottomFabScrim.setOnClickListener { closeMenu() }

        initialMenuDelegate?.let { showMenu(it, false) }
    }

    private fun closeMenu() {
        checkNotNull(menuDelegate)
        menuDelegate = null

        bottomBinding.apply {
            val transition = buildContainerTransformation()

            transition.startView = bottomFabMenu
            transition.endView = bottomFab

            transition.addTarget(bottomFab)

            TransitionManager.beginDelayedTransition(coordinatorLayout, transition)
            bottomFabMenu.visibility = View.INVISIBLE
            bottomFabScrim.visibility = View.INVISIBLE

            bottomFab.visibility = View.VISIBLE
        }
    }

    private fun buildContainerTransformation() = MaterialContainerTransform().apply {
        containerColor = MaterialColors.getColor(bottomBinding.root, R.attr.colorSecondary)
        scrimColor = Color.TRANSPARENT
        duration = 300
        interpolator = FastOutSlowInInterpolator()
        fadeMode = MaterialContainerTransform.FADE_MODE_IN
    }

    fun showMenu(menuDelegate: MenuDelegate, animate: Boolean = true) {
        check(this.menuDelegate == null)
        this.menuDelegate = menuDelegate

        bottomBinding.apply {
            bottomFabMenuList.removeAllViews()

            val items = menuDelegate.getItems()

            items.dropLast(1).forEach {
                ItemFabMenuBinding.inflate(
                    LayoutInflater.from(bottomFabMenu.context),
                    bottomFabMenuList,
                    true,
                )
                    .root
                    .apply {
                        text = it.getText(bottomFabMenu.context)

                        setOnClickListener { _ -> it.onClick(activity) }
                    }
            }

            items.last().let {
                bottomBinding.bottomFabMenuButton.apply {
                    text = it.getText(bottomFabMenu.context)

                    setOnClickListener { _ -> it.onClick(activity) }
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

            bottomFab.visibility = View.INVISIBLE
        }
    }

    fun getState() = menuDelegate

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
}