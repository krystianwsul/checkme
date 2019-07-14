package com.krystianwsul.checkme.viewmodels

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.firebase.ImageState
import com.krystianwsul.checkme.gui.tasks.TaskListFragment
import com.krystianwsul.checkme.utils.TaskKey

class ShowTaskViewModel : DomainViewModel<ShowTaskViewModel.Data>() {

    private lateinit var taskKey: TaskKey

    override val domainListener = object : DomainListener<Data>() {

        override fun getData(domainFactory: DomainFactory) = domainFactory.getShowTaskData(taskKey)
    }

    fun start(taskKey: TaskKey) {
        this.taskKey = taskKey

        internalStart()
    }

    data class Data(
            val name: String,
            val displayText: String?,
            val taskData: TaskListFragment.TaskData,
            val hasInstances: Boolean,
            val imageData: ImageState?,
            val current: Boolean) : DomainData() {

        init {
            check(name.isNotEmpty())
        }
    }
}