package com.krystianwsul.checkme.gui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import com.krystianwsul.checkme.databinding.FragmentProjectFilterDialogBinding
import com.krystianwsul.checkme.gui.base.NoCollapseBottomSheetDialogFragment
import com.krystianwsul.checkme.gui.utils.ResettableProperty

class ProjectFilterDialogFragment : NoCollapseBottomSheetDialogFragment() {

    companion object {

        fun newInstance() = ProjectFilterDialogFragment()
    }

    private var bindingProperty = ResettableProperty<FragmentProjectFilterDialogBinding>()
    private var binding by bindingProperty

    override val backgroundView get() = binding.projectFilterDialogRoot
    override val contentView get() = binding.projectFilterDialogContentWrapper

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        FragmentProjectFilterDialogBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onDestroyView() {
        bindingProperty.reset()

        super.onDestroyView()
    }
}