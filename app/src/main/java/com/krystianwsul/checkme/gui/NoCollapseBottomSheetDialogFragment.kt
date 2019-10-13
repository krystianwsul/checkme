package com.krystianwsul.checkme.gui

import android.os.Build
import android.os.Bundle
import android.view.View
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.utils.isLandscape
import com.krystianwsul.checkme.utils.setTransparentNavigation
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

        BottomSheetBehavior.from(dialog!!.window!!.findViewById<View>(R.id.design_bottom_sheet)).apply {
            skipCollapsed = true

            addBottomSheetCallback(bottomSheetCallback)

            if (first) {
                first = false

                if (state == BottomSheetBehavior.STATE_COLLAPSED && (alwaysExpand || landscape))
                    state = BottomSheetBehavior.STATE_EXPANDED
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            dialog!!.window!!
                    .decorView
                    .apply {
                        systemUiVisibility = systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
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

            window!!.setTransparentNavigation(landscape)
        }
    }
}