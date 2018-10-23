package com.krystianwsul.checkme.viewmodels

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.loaders.DomainData
import com.krystianwsul.checkme.loaders.FirebaseLevel
import java.util.*

class ProjectListViewModel : DomainViewModel<ProjectListViewModel.Data>() {

    fun start() = internalStart(FirebaseLevel.NEED)

    override fun getData(domainFactory: DomainFactory) = domainFactory.projectListData

    data class Data(val projectDatas: TreeMap<String, ProjectData>) : DomainData()

    data class ProjectData(val id: String, val name: String, val users: String) {

        init {
            check(id.isNotEmpty())
            check(name.isNotEmpty())
            check(users.isNotEmpty())
        }
    }
}