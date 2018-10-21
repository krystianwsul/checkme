package com.krystianwsul.checkme.viewmodels

import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.gui.tasks.TaskListFragment
import com.krystianwsul.checkme.loaders.FirebaseLevel

class MainViewModel : DomainViewModel<MainViewModel.Data>() {

    fun start() = internalStart(FirebaseLevel.WANT)

    override fun getData(domainFactory: DomainFactory) = domainFactory.getMainData(MyApplication.instance)
    data class Data(val taskData: TaskListFragment.TaskData) : com.krystianwsul.checkme.loaders.DomainData()
}