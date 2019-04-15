package com.krystianwsul.checkme.gui.tasks

import android.os.Bundle
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.gui.NoCollapseBottomSheetDialogFragment

class CameraGalleryFragment : NoCollapseBottomSheetDialogFragment() {

    companion object {

        fun newInstance() = CameraGalleryFragment()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?) = BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme).apply {
        setCancelable(true)
        setContentView(R.layout.dialog_camera_gallery)
    }
}