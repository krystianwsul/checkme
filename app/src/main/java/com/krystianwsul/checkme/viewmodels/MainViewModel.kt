package com.krystianwsul.checkme.viewmodels

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.gui.tasks.TaskListFragment

class MainViewModel : DomainViewModel<MainViewModel.Data>() {

    fun start() = internalStart()

    override fun getData(domainFactory: DomainFactory) = domainFactory.getMainData()

    data class Data(val taskData: TaskListFragment.TaskData) : DomainData()
}