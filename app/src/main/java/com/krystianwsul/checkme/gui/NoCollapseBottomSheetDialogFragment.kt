package com.krystianwsul.checkme.gui

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.CallSuper
import androidx.annotation.StyleRes
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.gui.utils.setNavBarTransparency
import com.krystianwsul.checkme.utils.isLandscape
import io.reactivex.disposables.CompositeDisposable

abstract class NoCollapseBottomSheetDialogFragment : BottomSheetDialogFragment() {

    private var first = true

    protected val viewCreatedDisposable = CompositeDisposable()

    protected open val alwaysExpand = false

    protected val landscape by lazy { resources.isLandscape }

    private val bottomSheetCallback = object : BottomSheetBehavior.BottomSheetCallback() {

        override fun onSlide(bottomSheet: View, slideOffset: Float) = Unit

        override fun onStateChanged(bottomSheet: View, newState: Int) {
            if (newState == BottomSheetBehavior.STATE_HIDDEN)
                dialog!!.cancel()
        }
    }

    @StyleRes
    protected open val dialogStyle = R.style.BottomSheetDialogTheme

    protected abstract val backgroundView: View
    protected abstract val contentView: View

    final override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = TransparentNavigationDialog()

    @CallSuper
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        ViewCompat.setOnApplyWindowInsetsListener(backgroundView) { _, windowInsetsCompat ->
            val mask = WindowInsetsCompat.Type.systemBars()

            val insets = windowInsetsCompat.getInsets(mask)

            Log.e("asdf", "insets " + insets.left + "-" + insets.top + "-" + insets.right + "-" + insets.bottom)

            backgroundView.updateLayoutParams<FrameLayout.LayoutParams> {
                leftMargin = insets.left
                topMargin = insets.top
                rightMargin = insets.right
            }

            contentView.setPadding(
                    0,
                    0,
                    0,
                    insets.bottom
            )

            // todo check if this is necessary
            requireDialog().findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)!!.setPadding(0, 0, 0, 0)

            WindowInsetsCompat.CONSUMED
        }
    }

    override fun onStart() {
        super.onStart()

        BottomSheetBehavior.from(requireDialog().window!!.findViewById(R.id.design_bottom_sheet)).apply {
            skipCollapsed = true

            addBottomSheetCallback(bottomSheetCallback)

            if (first) {
                first = false

                if (state == BottomSheetBehavior.STATE_COLLAPSED && (alwaysExpand || landscape))
                    state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
    }

    override fun onDestroyView() {
        viewCreatedDisposable.clear()

        super.onDestroyView()
    }

    private inner class TransparentNavigationDialog : BottomSheetDialog(requireContext(), dialogStyle) {

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            // todo check if this is necessary
            findViewById<View>(com.google.android.material.R.id.container)!!.fitsSystemWindows = false
            findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)!!.setPadding(0, 0, 0, 0)

            setNavBarTransparency(window!!, backgroundView, landscape)
        }
    }
}