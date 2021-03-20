package com.krystianwsul.checkme.viewmodels

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.extensions.getMainData
import com.krystianwsul.checkme.gui.tasks.TaskListFragment

class MainViewModel : DomainViewModel<MainViewModel.Data>() {

    override val domainListener = object : DomainListener<Data>() {

        override fun getData(domainFactory: DomainFactory) = domainFactory.getMainData()
    }

    fun start() = internalStart()

    data class Data(val taskData: TaskListFragment.TaskData) : DomainData()
}