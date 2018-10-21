package com.krystianwsul.checkme.loaders

import android.content.Context

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.gui.tasks.TaskListFragment

class MainLoader(context: Context) : DomainLoader<MainLoader.DomainData>(context, FirebaseLevel.WANT) {

    override val name = "MainLoader"

    override fun loadDomain(domainFactory: DomainFactory) = domainFactory.getMainData(context)

    data class DomainData(val taskData: TaskListFragment.TaskData) : com.krystianwsul.checkme.loaders.DomainData()
}
