package com.krystianwsul.checkme.gui

import android.content.res.Configuration
import android.view.View
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.krystianwsul.checkme.R

abstract class NoCollapseBottomSheetDialogFragment : BottomSheetDialogFragment() {

    private var first = true

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

                if (state == BottomSheetBehavior.STATE_COLLAPSED && resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
                    state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
    }
}