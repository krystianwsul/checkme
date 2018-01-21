package com.krystianwsul.checkme.loaders

import android.content.Context

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.gui.tasks.TaskListFragment

class MainLoader(context: Context) : DomainLoader<MainLoader.Data>(context, DomainLoader.FirebaseLevel.WANT) {

    override fun getName() = "MainLoader"

    override fun loadDomain(domainFactory: DomainFactory) = domainFactory.getMainData(context)

    data class Data(val taskData: TaskListFragment.TaskData) : DomainLoader.Data()
}
