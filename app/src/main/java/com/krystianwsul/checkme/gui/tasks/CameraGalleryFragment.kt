package com.krystianwsul.checkme.gui.tasks

import android.os.Bundle
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.navigation.NavigationView
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.gui.NoCollapseBottomSheetDialogFragment

class CameraGalleryFragment : NoCollapseBottomSheetDialogFragment() {

    companion object {

        private const val KEY_SHOW_REMOVE = "showRemove"

        fun newInstance(showRemove: Boolean) = CameraGalleryFragment().apply {
            arguments = Bundle().apply {
                putBoolean(KEY_SHOW_REMOVE, showRemove)
            }
        }
    }

    private var showRemove = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        showRemove = arguments!!.getBoolean(KEY_SHOW_REMOVE)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?) = BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme).apply {
        setCancelable(true)
        setContentView(R.layout.dialog_camera_gallery)
    }

    override fun onStart() {
        super.onStart()

        dialog!!.findViewById<NavigationView>(R.id.cameraGalleryNavigation)
                .menu
                .findItem(R.id.camera_gallery_remove)
                .isVisible = showRemove
    }
}