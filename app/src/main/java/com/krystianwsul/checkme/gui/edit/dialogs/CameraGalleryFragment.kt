package com.krystianwsul.checkme.gui.edit.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import com.google.android.material.navigation.NavigationView
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.databinding.DialogCameraGalleryBinding
import com.krystianwsul.checkme.gui.base.NoCollapseBottomSheetDialogFragment
import com.krystianwsul.checkme.gui.edit.EditActivity
import com.krystianwsul.checkme.gui.edit.EditImageState
import com.krystianwsul.checkme.gui.utils.ResettableProperty
import com.krystianwsul.checkme.utils.toV3
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

    override val backgroundView get() = binding.cameraGalleryRoot
    override val contentView get() = binding.cameraGalleryBackground

    private var showRemove = false

    private val editActivity get() = activity as EditActivity

    private val bindingProperty = ResettableProperty<DialogCameraGalleryBinding>()
    private var binding by bindingProperty

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        showRemove = requireArguments().getBoolean(KEY_SHOW_REMOVE)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = DialogCameraGalleryBinding.inflate(inflater, container, false).also { binding = it }.root

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
                                        .toV3()
                        )

                    }
                    R.id.camera_gallery_gallery -> {
                        editActivity.getImage(
                                RxPaparazzo.single(editActivity)
                                        .size(ScreenSize())
                                        .useInternalStorage()
                                        .usingGallery()
                                        .toV3()
                        )

                    }
                    R.id.camera_gallery_remove -> editActivity.editViewModel.setEditImageState(EditImageState.Removed)
                    else -> throw IllegalArgumentException()
                }

                dismiss()

                true
            }
        }
    }

    override fun onDestroyView() {
        bindingProperty.reset()

        super.onDestroyView()
    }
}