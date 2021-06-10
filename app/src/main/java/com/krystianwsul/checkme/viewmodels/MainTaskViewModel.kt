package com.krystianwsul.checkme.viewmodels

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.extensions.getMainTaskData
import com.krystianwsul.checkme.gui.tasks.TaskListFragment

class MainTaskViewModel : DomainViewModel<MainTaskViewModel.Data>() {

    override val domainListener = object : DomainListener<Data>() {

        override fun getData(domainFactory: DomainFactory) = domainFactory.getMainTaskData()
    }

    fun start() = internalStart()

    data class Data(val taskData: TaskListFragment.TaskData) : DomainData()
}