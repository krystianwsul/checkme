package com.krystianwsul.checkme.gui

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.krystianwsul.checkme.R
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

    inner class TransparentNavigationDialog : BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme_Navbar) {

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            findViewById<View>(com.google.android.material.R.id.container)!!.fitsSystemWindows = false
            findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)!!.setPadding(0, 0, 0, 0)

            window!!.apply {
                var flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE

                if (landscape)
                    navigationBarColor = ContextCompat.getColor(context, R.color.primaryColor12Solid)
                else
                    flags = flags or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    flags = flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR

                decorView.systemUiVisibility = decorView.systemUiVisibility or flags
            }
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