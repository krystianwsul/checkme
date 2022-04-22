package com.krystianwsul.checkme.viewmodels

import com.jakewharton.rxrelay3.PublishRelay
import com.krystianwsul.checkme.domainmodel.extensions.getShowTaskData
import com.krystianwsul.checkme.firebase.database.DomainFactoryInitializationDelayProvider
import com.krystianwsul.checkme.firebase.database.TaskPriorityMapper
import com.krystianwsul.checkme.gui.tasks.TaskListFragment
import com.krystianwsul.common.criteria.SearchCriteria
import com.krystianwsul.common.firebase.models.ImageState
import com.krystianwsul.common.utils.TaskKey
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.kotlin.addTo

class ShowTaskViewModel : ObservableDomainViewModel<ShowTaskViewModel.Data, ShowTaskViewModel.Parameters>() {

    override val domainListener = object : DomainListener<Data>() {

        override val domainResultFetcher =
            DomainResultFetcher.DomainFactoryData { it.getShowTaskData(parameters.taskKey, parameters.searchCriteria) }

        override fun newDelayProvider() = DomainFactoryInitializationDelayProvider.Task.fromTaskKey(parameters.taskKey)

        override fun newTaskPriorityMapper() =
            TaskPriorityMapper.PrioritizeTaskWithDependencies.fromTaskKey(parameters.taskKey)
    }

    private val taskKeyRelay = PublishRelay.create<TaskKey>()

    val searchRelay = PublishRelay.create<SearchCriteria.Search.Query>()

    init {
        Observable.combineLatest(taskKeyRelay, searchRelay) { taskKey, search ->
            Parameters(taskKey, SearchCriteria(search, showDeleted = false))
        }
            .subscribe(parametersRelay)
            .addTo(clearedDisposable)
    }

    fun start(taskKey: TaskKey) {
        taskKeyRelay.accept(taskKey)

        internalStart()
    }

    data class Data(
        val name: String,
        val collapseText: String?,
        val taskData: TaskListFragment.TaskData,
        val imageData: ImageState?,
        val current: Boolean,
        val canMigrateDescription: Boolean,
        val newTaskKey: TaskKey,
    ) : DomainData() {

        init {
            check(name.isNotEmpty())
        }
    }

    data class Parameters(val taskKey: TaskKey, val searchCriteria: SearchCriteria) : ObservableDomainViewModel.Parameters
}