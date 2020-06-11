package com.krystianwsul.checkme.viewmodels

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.extensions.getProjectListData
import com.krystianwsul.common.utils.ProjectKey
import java.util.*

class ProjectListViewModel : DomainViewModel<ProjectListViewModel.Data>() {

    override val domainListener = object : DomainListener<Data>() {

        override fun getData(domainFactory: DomainFactory) = domainFactory.getProjectListData()
    }

    fun start() = internalStart()

    data class Data(val projectDatas: SortedMap<ProjectKey.Shared, ProjectData>) : DomainData()

    data class ProjectData(val id: ProjectKey.Shared, val name: String, val users: String) {

        init {
            check(name.isNotEmpty())
            check(users.isNotEmpty())
        }
    }
}