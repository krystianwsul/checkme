package com.krystianwsul.checkme.gui.main

import android.content.Context
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
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.databinding.FragmentProjectFilterDialogBinding
import com.krystianwsul.checkme.gui.base.NoCollapseBottomSheetDialogFragment
import com.krystianwsul.checkme.gui.utils.ResettableProperty
import com.krystianwsul.checkme.viewmodels.getViewModel
import io.reactivex.rxjava3.kotlin.addTo

class ProjectFilterDialogFragment : NoCollapseBottomSheetDialogFragment() {

    companion object {

        fun newInstance() = ProjectFilterDialogFragment()
    }

    private var bindingProperty = ResettableProperty<FragmentProjectFilterDialogBinding>()
    private var binding by bindingProperty

    override val backgroundView get() = binding.projectFilterDialogRoot
    override val contentView get() = binding.projectFilterDialogContentWrapper

    private val viewModel by lazy { getViewModel<ProjectFilterViewModel>() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.start()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        FragmentProjectFilterDialogBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // todo filter add initial progress bar view

        viewModel.data
            .subscribe {
                val items = listOf(Item.All, Item.Private) + it.projects.map { Item.Shared(it) }

                binding.projectFilterDialogCompose.setContent {
                    MdcTheme {
                        ProjectList(items) {
                            // todo filter save to preferences
                        }
                    }
                }
            }
            .addTo(viewCreatedDisposable)
    }

    @Composable
    private fun ProjectList(items: List<Item>, onClick: (Item) -> Unit) {
        Column {
            items.forEach { item ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = false, onClick = { onClick(item) })

                    val annotatedString = buildAnnotatedString {
                        append(item.getName(requireContext()))
                    }

                    ClickableText(text = annotatedString, onClick = { onClick(item) })
                }
            }
        }
    }

    override fun onDestroyView() {
        bindingProperty.reset()

        super.onDestroyView()
    }

    sealed class Item {

        abstract fun getName(context: Context): String

        object All : Item() {

            override fun getName(context: Context) = context.getString(R.string.allProjects)
        }

        object Private : Item() {

            override fun getName(context: Context) = context.getString(R.string.myTasks)
        }

        class Shared(private val project: ProjectFilterViewModel.Project) : Item() {

            override fun getName(context: Context) = project.name
        }
    }
}