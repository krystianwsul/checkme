package com.krystianwsul.checkme.viewmodels

import com.krystianwsul.checkme.domainmodel.DomainFactory
import java.util.*

class ProjectListViewModel : DomainViewModel<ProjectListViewModel.Data>() {

    fun start() = internalStart()

    override fun getData(domainFactory: DomainFactory) = domainFactory.getProjectListData()

    data class Data(val projectDatas: SortedMap<String, ProjectData>) : DomainData()

    data class ProjectData(val id: String, val name: String, val users: String) {

        init {
            check(id.isNotEmpty())
            check(name.isNotEmpty())
            check(users.isNotEmpty())
        }
    }
}