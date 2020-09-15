package com.krystianwsul.checkme.gui.edit.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import com.google.android.material.navigation.NavigationView
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.gui.NoCollapseBottomSheetDialogFragment
import com.krystianwsul.checkme.gui.edit.EditActivity
import com.krystianwsul.checkme.gui.edit.EditImageState
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

    private val editActivity get() = activity as EditActivity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        showRemove = requireArguments().getBoolean(KEY_SHOW_REMOVE)
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ) = inflater.inflate(R.layout.dialog_camera_gallery, container, false)!!

    override fun onStart() {
        super.onStart()

        dialog!!.findViewById<NavigationView>(R.id.cameraGalleryNavigation)!!.apply {
            menu.findItem(R.id.camera_gallery_remove).isVisible = showRemove

            setNavigationItemSelectedListener {
                when (it.itemId) {
                    R.id.camera_gallery_camera -> {
                        editActivity.getImage(
                                RxPaparazzo.single(editActivity)
                                        .size(ScreenSize())
                                        .useInternalStorage()
                                        .usingCamera()
                        )

                    }
                    R.id.camera_gallery_gallery -> {
                        editActivity.getImage(
                                RxPaparazzo.single(editActivity)
                                        .size(ScreenSize())
                                        .useInternalStorage()
                                        .usingGallery()
                        )

                    }
                    R.id.camera_gallery_remove -> editActivity.delegate
                            .imageUrl
                            .accept(EditImageState.Removed)
                    else -> throw IllegalArgumentException()
                }

                dismiss()

                true
            }
        }
    }
}