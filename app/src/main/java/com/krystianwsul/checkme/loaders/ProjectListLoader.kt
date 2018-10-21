package com.krystianwsul.checkme.loaders

import android.content.Context
import com.krystianwsul.checkme.domainmodel.DomainFactory

import java.util.*

class ProjectListLoader(context: Context) : DomainLoader<ProjectListLoader.DomainData>(context, FirebaseLevel.NEED) {

    override val name = "ProjectListLoader"

    override fun loadDomain(domainFactory: DomainFactory) = domainFactory.projectListData

    data class DomainData(val projectDatas: TreeMap<String, ProjectData>) : com.krystianwsul.checkme.loaders.DomainData()

    data class ProjectData(val id: String, val name: String, val users: String) {

        init {
            check(id.isNotEmpty())
            check(name.isNotEmpty())
            check(users.isNotEmpty())
        }
    }
}
