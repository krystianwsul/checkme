package com.krystianwsul.checkme.viewmodels

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.gui.tasks.TaskListFragment
import com.krystianwsul.checkme.utils.TaskKey

class ShowTaskViewModel : DomainViewModel<ShowTaskViewModel.Data>() {

    private lateinit var taskKey: TaskKey

    fun start(taskKey: TaskKey) {
        this.taskKey = taskKey

        internalStart(if (taskKey.type == TaskKey.Type.REMOTE) FirebaseLevel.NEED else FirebaseLevel.NOTHING)
    }

    override fun getData(domainFactory: DomainFactory) = domainFactory.getShowTaskData(taskKey)

    data class Data(
            val name: String,
            val scheduleText: String?,
            val taskData: TaskListFragment.TaskData,
            val hasInstances: Boolean) : DomainData() {

        init {
            check(name.isNotEmpty())
        }
    }
}