package com.krystianwsul.checkme.viewmodels

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.extensions.getShowTasksData
import com.krystianwsul.checkme.gui.tasks.ShowTasksActivity
import com.krystianwsul.checkme.gui.tasks.TaskListFragment

class ShowTasksViewModel : DomainViewModel<ShowTasksViewModel.Data>() {

    override val domainListener = object : DomainListener<Data>() {

        override fun getData(domainFactory: DomainFactory) = domainFactory.getShowTasksData(parameters)
    }

    private lateinit var parameters: ShowTasksActivity.Parameters

    fun start(parameters: ShowTasksActivity.Parameters) {
        this.parameters = parameters

        internalStart()
    }

    data class Data(val taskData: TaskListFragment.TaskData, val title: String) : DomainData()
}