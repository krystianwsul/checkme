package com.krystianwsul.checkme.gui.main

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.buildAnnotatedString
import com.google.android.material.composethemeadapter.MdcTheme
import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.databinding.FragmentProjectFilterDialogBinding
import com.krystianwsul.checkme.gui.base.NoCollapseBottomSheetDialogFragment
import com.krystianwsul.checkme.gui.utils.ResettableProperty
import com.krystianwsul.checkme.viewmodels.getViewModel
import com.krystianwsul.common.utils.NullableWrapper
import io.reactivex.rxjava3.kotlin.Observables
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

        // the preference could actually just be fetched imperatively, but I already wrote the code so whatever
        Observables.combineLatest(viewModel.data, Preferences.projectFilterObservable)
            .map { (data, projectFilter) ->
                val items = listOf(Item.All, Item.Private) + data.projects.map { Item.Shared(it) }

                val selected = if (
                    projectFilter is Preferences.ProjectFilter.Shared &&
                    data.projects.none { it.projectKey == projectFilter.projectKey }
                ) {
                    Preferences.ProjectFilter.All
                } else {
                    projectFilter
                }

                NullableWrapper(Pair(items, selected))
            }
            .startWithItem(NullableWrapper())
            .subscribe {
                binding.projectFilterDialogCompose.setContent {
                    MdcTheme {
                        ProjectList(it.value) {
                            Preferences.projectFilter = it.projectFilter
                            dismiss()
                        }
                    }
                }
            }
            .addTo(viewCreatedDisposable)
    }

    @Composable
    private fun ProjectList(pair: Pair<List<Item>, Preferences.ProjectFilter>?, onClick: (Item) -> Unit) {
        val itemState = derivedStateOf { pair }

        // todo these animations should be disabled based on data.immediate, but IDK how to do that right now
        Box(Modifier.animateContentSize()) {
            Crossfade(targetState = itemState, modifier = Modifier.fillMaxWidth()) {
                val currentPair = it.value

                if (currentPair == null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        CircularProgressIndicator(color = MaterialTheme.colors.secondary)
                    }
                } else {
                    Column {
                        val (items, selected) = currentPair

                        items.forEach { item ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = item.projectFilter == selected, onClick = { onClick(item) })

                                val annotatedString = buildAnnotatedString {
                                    append(item.getName(requireContext()))
                                }

                                ClickableText(text = annotatedString, onClick = { onClick(item) })
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        bindingProperty.reset()

        super.onDestroyView()
    }

    sealed class Item {

        abstract val projectFilter: Preferences.ProjectFilter

        abstract fun getName(context: Context): String

        object All : Item() {

            override val projectFilter = Preferences.ProjectFilter.All

            override fun getName(context: Context) = context.getString(R.string.allProjects)
        }

        object Private : Item() {

            override val projectFilter = Preferences.ProjectFilter.Private

            override fun getName(context: Context) = context.getString(R.string.myTasks)
        }

        class Shared(private val project: ProjectFilterViewModel.Project) : Item() {

            override val projectFilter = Preferences.ProjectFilter.Shared(project.projectKey)

            override fun getName(context: Context) = project.name
        }
    }
}