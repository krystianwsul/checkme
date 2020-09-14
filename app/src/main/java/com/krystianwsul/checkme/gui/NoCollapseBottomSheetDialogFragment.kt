package com.krystianwsul.checkme.gui

import android.os.Bundle
import android.view.View
import androidx.annotation.StyleRes
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.gui.utils.setNavBarColor
import com.krystianwsul.checkme.gui.utils.setNavBarTransparency
import com.krystianwsul.checkme.utils.isLandscape
import io.reactivex.disposables.CompositeDisposable

abstract class NoCollapseBottomSheetDialogFragment : BottomSheetDialogFragment() {

    private var first = true

    protected val startDisposable = CompositeDisposable()

    protected open val alwaysExpand = false

    protected val landscape by lazy { resources.isLandscape }

    private val bottomSheetCallback = object : BottomSheetBehavior.BottomSheetCallback() {

        override fun onSlide(bottomSheet: View, slideOffset: Float) = Unit

        override fun onStateChanged(bottomSheet: View, newState: Int) {
            if (newState == BottomSheetBehavior.STATE_HIDDEN)
                dialog!!.cancel()
        }
    }

    override fun onStart() {
        super.onStart()

        BottomSheetBehavior.from(requireDialog().window!!.findViewById<View>(R.id.design_bottom_sheet)).apply {
            skipCollapsed = true

            addBottomSheetCallback(bottomSheetCallback)

            if (first) {
                first = false

                if (state == BottomSheetBehavior.STATE_COLLAPSED && (alwaysExpand || landscape))
                    state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
    }

    override fun onStop() {
        startDisposable.clear()

        super.onStop()
    }

    inner class TransparentNavigationDialog(
            @StyleRes styleId: Int = R.style.BottomSheetDialogTheme
    ) : BottomSheetDialog(requireContext(), styleId) {

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            findViewById<View>(com.google.android.material.R.id.container)!!.fitsSystemWindows = false
            findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)!!.setPadding(0, 0, 0, 0)

            setNavBarTransparency(window!!, landscape)
        }

        override fun setContentView(view: View) {
            super.setContentView(view)

            setNavBarColor(window!!, view, landscape)
        }

        fun setInsetViews(outer: View, inner: View) {
            outer.setOnApplyWindowInsetsListener { _, insets ->
                outer.setPadding(0, insets.systemWindowInsetTop, 0, 0)

                inner.setPadding(
                        insets.systemWindowInsetLeft,
                        0,
                        insets.systemWindowInsetRight,
                        insets.systemWindowInsetBottom
                )

                insets.consumeSystemWindowInsets()
            }

            outer.requestApplyInsets()
        }
    }
}