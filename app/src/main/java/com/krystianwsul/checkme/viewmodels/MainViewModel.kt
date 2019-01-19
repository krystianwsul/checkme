package com.krystianwsul.checkme.viewmodels

import com.krystianwsul.checkme.gui.tasks.TaskListFragment

class MainViewModel : DomainViewModel<MainViewModel.Data>() {

    fun start() = internalStart()

    override fun getData() = domainFactory.getMainData()

    data class Data(val taskData: TaskListFragment.TaskData) : DomainData()
}