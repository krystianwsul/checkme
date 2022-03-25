package com.krystianwsul.checkme.viewmodels

import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.domainmodel.extensions.getMainTaskData
import com.krystianwsul.checkme.gui.tasks.TaskListFragment
import io.reactivex.rxjava3.kotlin.addTo
import kotlin.properties.Delegates

class MainTaskViewModel : DomainViewModel<MainTaskViewModel.Data>() {

    private var showProjects by Delegates.notNull<Boolean>()

    override val domainListener = object : DomainListener<Data>() {

        override val domainResultFetcher = DomainResultFetcher.DomainFactoryData { it.getMainTaskData(showProjects) }
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