package com.krystianwsul.checkme.gui

import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.View
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.krystianwsul.checkme.R
import io.reactivex.disposables.CompositeDisposable

abstract class NoCollapseBottomSheetDialogFragment : BottomSheetDialogFragment() {

    private var first = true

    protected val startDisposable = CompositeDisposable()

    protected open val alwaysExpand = false

    override fun onStart() {
        super.onStart()

        BottomSheetBehavior.from(dialog!!.window!!.findViewById<View>(R.id.design_bottom_sheet)).apply {
            skipCollapsed = true

            setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {

                override fun onSlide(bottomSheet: View, slideOffset: Float) = Unit

                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    if (newState == BottomSheetBehavior.STATE_HIDDEN)
                        dialog!!.cancel()
                }
            })

            if (first) {
                first = false

                if (state == BottomSheetBehavior.STATE_COLLAPSED && (alwaysExpand || resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE))
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

            window!!.apply {
                var flags = decorView.systemUiVisibility or
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    flags = flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR

                decorView.systemUiVisibility = flags
            }
        }

    }
}