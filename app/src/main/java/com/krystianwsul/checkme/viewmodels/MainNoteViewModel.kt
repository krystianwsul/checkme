package com.krystianwsul.checkme.viewmodels

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.extensions.getMainNoteData
import com.krystianwsul.checkme.gui.tasks.TaskListFragment

class MainNoteViewModel : DomainViewModel<MainNoteViewModel.Data>() {

    override val domainListener = object : DomainListener<Data>() {

        override fun getData(domainFactory: DomainFactory) = domainFactory.getMainNoteData()
    }

    fun start() = internalStart()

    data class Data(val taskData: TaskListFragment.TaskData) : DomainData()
}