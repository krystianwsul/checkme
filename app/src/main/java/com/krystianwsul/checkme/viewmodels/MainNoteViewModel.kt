package com.krystianwsul.checkme.viewmodels

import com.krystianwsul.checkme.domainmodel.extensions.getMainNoteData
import com.krystianwsul.checkme.gui.tasks.TaskListFragment

class MainNoteViewModel : DomainViewModel<MainNoteViewModel.Data>() {

    override val domainListener = object : DomainListener<Data>() {

        override val domainResultFetcher = DomainResultFetcher.DomainFactoryData { it.getMainNoteData() }
    }

    fun start() = internalStart()

    data class Data(val taskData: TaskListFragment.TaskData) : DomainData()
}