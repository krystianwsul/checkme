package com.krystianwsul.checkme.gui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.buildAnnotatedString
import com.google.android.material.composethemeadapter.MdcTheme
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val projects = (0..10).map { "project $it" }

        binding.projectFilterDialogCompose.setContent {
            MdcTheme { ProjectList(projects) { } }
        }
    }

    @Composable
    private fun ProjectList(projects: List<String>, onClick: (String) -> Unit) {
        Column {
            projects.forEach { project ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = false, onClick = { onClick(project) })

                    val annotatedString = buildAnnotatedString {
                        append(project)
                    }

                    ClickableText(text = annotatedString, onClick = { onClick(project) })
                }
            }
        }
    }

    override fun onDestroyView() {
        bindingProperty.reset()

        super.onDestroyView()
    }
}