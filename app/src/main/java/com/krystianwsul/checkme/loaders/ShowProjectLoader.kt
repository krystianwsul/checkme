package com.krystianwsul.checkme.loaders

import android.content.Context

import com.krystianwsul.checkme.domainmodel.DomainFactory

import junit.framework.Assert

class ShowProjectLoader(context: Context, private val projectId: String?) : DomainLoader<ShowProjectLoader.Data>(context, DomainLoader.FirebaseLevel.FRIEND) {

    override fun getName() = "ShowProjectLoader, projectId: " + projectId

    override fun loadDomain(domainFactory: DomainFactory) = domainFactory.getShowProjectData(projectId)

    data class Data(val name: String?, val userListDatas: Set<UserListData>, val friendDatas: Map<String, UserListData>) : DomainLoader.Data()

    data class UserListData(val name: String, val email: String, val id: String) {

        init {
            Assert.assertTrue(name.isNotEmpty())
            Assert.assertTrue(email.isNotEmpty())
            Assert.assertTrue(id.isNotEmpty())
        }
    }
}
