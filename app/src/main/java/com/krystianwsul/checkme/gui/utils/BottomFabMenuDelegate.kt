package com.krystianwsul.checkme.gui.utils

import android.app.Activity
import android.graphics.Color
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.transition.TransitionManager
import com.google.android.material.color.MaterialColors
import com.google.android.material.transition.MaterialContainerTransform
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.databinding.BottomBinding

class BottomFabMenuDelegate(
    private val bottomBinding: BottomBinding,
    private val coordinatorLayout: CoordinatorLayout,
    private val activity: Activity,
) {

    init {
        bottomBinding.apply {
            bottomFabScrim.setOnClickListener {
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
    }

    private fun buildContainerTransformation() = MaterialContainerTransform().apply {
        containerColor = MaterialColors.getColor(bottomBinding.root, R.attr.colorSecondary)
        scrimColor = Color.TRANSPARENT
        duration = 300
        interpolator = FastOutSlowInInterpolator()
        fadeMode = MaterialContainerTransform.FADE_MODE_IN
    }

    fun showMenu() {
        bottomBinding.apply {
            val transition = buildContainerTransformation()

            transition.startView = bottomFab
            transition.endView = bottomFabMenu

            transition.addTarget(bottomFabMenu)

            TransitionManager.beginDelayedTransition(activity.findViewById(android.R.id.content), transition)
            bottomFabMenu.visibility = View.VISIBLE
            bottomFabScrim.visibility = View.VISIBLE

            bottomFab.visibility = View.INVISIBLE
        }
    }
}