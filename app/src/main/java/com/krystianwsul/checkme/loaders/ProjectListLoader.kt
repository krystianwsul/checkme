package com.krystianwsul.checkme.loaders

import android.content.Context
import com.krystianwsul.checkme.domainmodel.DomainFactory

import java.util.*

class ProjectListLoader(context: Context) : DomainLoader<ProjectListLoader.Data>(context, DomainLoader.FirebaseLevel.NEED) {

    override val name = "ProjectListLoader"

    override fun loadDomain(domainFactory: DomainFactory) = domainFactory.projectListData

    data class Data(val projectDatas: TreeMap<String, ProjectData>) : DomainLoader.Data()

    data class ProjectData(val id: String, val name: String, val users: String) {

        init {
            check(id.isNotEmpty())
            check(name.isNotEmpty())
            check(users.isNotEmpty())
        }
    }
}
