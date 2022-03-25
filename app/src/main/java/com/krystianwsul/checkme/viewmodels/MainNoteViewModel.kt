package com.krystianwsul.checkme.viewmodels

import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.domainmodel.extensions.getMainNoteData
import com.krystianwsul.checkme.gui.tasks.TaskListFragment
import io.reactivex.rxjava3.kotlin.addTo
import kotlin.properties.Delegates.notNull

class MainNoteViewModel : DomainViewModel<MainNoteViewModel.Data>() {

    private var showProjects by notNull<Boolean>()

    override val domainListener = object : DomainListener<Data>() {

        override val domainResultFetcher = DomainResultFetcher.DomainFactoryData { it.getMainNoteData(showProjects) }
    }

    fun start() {
        if (started) return

        Preferences.showProjectsObservable
            .subscribe {
                showProjects = it
                refresh()
            }
            .addTo(startedDisposable)
    }

    data class Data(val taskData: TaskListFragment.TaskData) : DomainData()
}