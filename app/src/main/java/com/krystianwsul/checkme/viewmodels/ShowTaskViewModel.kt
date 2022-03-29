package com.krystianwsul.checkme.viewmodels

import com.krystianwsul.checkme.domainmodel.extensions.getShowTaskData
import com.krystianwsul.checkme.gui.tasks.TaskListFragment
import com.krystianwsul.common.firebase.models.ImageState
import com.krystianwsul.common.utils.TaskKey

class ShowTaskViewModel : DomainViewModel<ShowTaskViewModel.Data>() {

    private lateinit var taskKey: TaskKey

    override val domainListener = object : DomainListener<Data>() {

        override val domainResultFetcher = DomainResultFetcher.DomainFactoryData { it.getShowTaskData(taskKey) }
    }

    fun start(taskKey: TaskKey) { // todo show done
        this.taskKey = taskKey

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
}