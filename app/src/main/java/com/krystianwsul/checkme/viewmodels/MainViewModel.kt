package com.krystianwsul.checkme.viewmodels

import com.krystianwsul.checkme.gui.tasks.TaskListFragment

class MainViewModel : DomainViewModel<MainViewModel.Data>() {

    fun start() = internalStart(FirebaseLevel.WANT)

    override fun getData() = domainFactory.mainData

    data class Data(val taskData: TaskListFragment.TaskData) : DomainData()
}