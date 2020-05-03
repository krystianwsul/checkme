package com.krystianwsul.checkme.gui.tasks

import android.os.Bundle
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.navigation.NavigationView
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.gui.NoCollapseBottomSheetDialogFragment
import com.krystianwsul.checkme.gui.tasks.create.CreateTaskActivity
import com.krystianwsul.checkme.gui.tasks.create.CreateTaskImageState
import com.miguelbcr.ui.rx_paparazzo2.RxPaparazzo
import com.miguelbcr.ui.rx_paparazzo2.entities.size.ScreenSize

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

    private val createTaskActivity get() = activity as CreateTaskActivity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        showRemove = requireArguments().getBoolean(KEY_SHOW_REMOVE)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?) = BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme).apply {
        setCancelable(true)
        setContentView(R.layout.dialog_camera_gallery)
    }

    override fun onStart() {
        super.onStart()

        dialog!!.findViewById<NavigationView>(R.id.cameraGalleryNavigation)!!.apply {
            menu.findItem(R.id.camera_gallery_remove).isVisible = showRemove

            setNavigationItemSelectedListener {
                when (it.itemId) {
                    R.id.camera_gallery_camera -> {
                        createTaskActivity.getImage(
                                RxPaparazzo.single(createTaskActivity)
                                        .size(ScreenSize())
                                        .useInternalStorage()
                                        .usingCamera()
                        )

                    }
                    R.id.camera_gallery_gallery -> {
                        createTaskActivity.getImage(
                                RxPaparazzo.single(createTaskActivity)
                                        .size(ScreenSize())
                                        .useInternalStorage()
                                        .usingGallery()
                        )

                    }
                    R.id.camera_gallery_remove -> createTaskActivity.imageUrl.accept(CreateTaskImageState.Removed)
                    else -> throw IllegalArgumentException()
                }

                dismiss()

                true
            }
        }
    }
}