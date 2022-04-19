package com.krystianwsul.checkme.viewmodels

import com.jakewharton.rxrelay3.PublishRelay
import com.krystianwsul.checkme.domainmodel.HasInstancesStore
import com.krystianwsul.checkme.domainmodel.extensions.getShowTaskData
import com.krystianwsul.checkme.gui.tasks.TaskListFragment
import com.krystianwsul.common.criteria.SearchCriteria
import com.krystianwsul.common.firebase.models.ImageState
import com.krystianwsul.common.utils.TaskKey
import com.mindorks.scheduler.Priority
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.kotlin.addTo

class ShowTaskViewModel : ObservableDomainViewModel<ShowTaskViewModel.Data, ShowTaskViewModel.Parameters>() {

    override val domainListener = object : DomainListener<Data>() {

        /*
        Awful hack to prevent ShowTaskActivity from crashing on startup.  Definitely restructure into something where
        DatabaseResultQueue can examine the currently registered DomainListeners, and determine appropriate load priority
        based on that.
         */
        override val priority
            get() = when (val taskKey = parameters.taskKey) {
                is TaskKey.Project -> super.priority
                is TaskKey.Root -> {
                    val taskPriority = HasInstancesStore.getTaskPriority(taskKey)

                    Priority.values()[taskPriority.ordinal - 1]
                }
            }

        override val domainResultFetcher =
            DomainResultFetcher.DomainFactoryData { it.getShowTaskData(parameters.taskKey, parameters.searchCriteria) }
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