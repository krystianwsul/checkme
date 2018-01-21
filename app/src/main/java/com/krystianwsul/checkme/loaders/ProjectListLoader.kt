package com.krystianwsul.checkme.loaders

import android.content.Context
import com.krystianwsul.checkme.domainmodel.DomainFactory
import junit.framework.Assert
import java.util.*

class ProjectListLoader(context: Context) : DomainLoader<ProjectListLoader.Data>(context, DomainLoader.FirebaseLevel.NEED) {

    override fun getName() = "ProjectListLoader"

    override fun loadDomain(domainFactory: DomainFactory) = domainFactory.projectListData

    data class Data(val projectDatas: TreeMap<String, ProjectData>) : DomainLoader.Data()

    data class ProjectData(val id: String, val name: String, val users: String) {

        init {
            Assert.assertTrue(id.isNotEmpty())
            Assert.assertTrue(name.isNotEmpty())
            Assert.assertTrue(users.isNotEmpty())
        }
    }
}
