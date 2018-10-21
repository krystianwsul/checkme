package com.krystianwsul.checkme.loaders

import android.content.Context

import com.krystianwsul.checkme.domainmodel.DomainFactory


class ShowProjectLoader(context: Context, private val projectId: String?) : DomainLoader<ShowProjectLoader.DomainData>(context, FirebaseLevel.FRIEND) {

    override val name = "ShowProjectLoader, projectId: " + projectId

    override fun loadDomain(domainFactory: DomainFactory) = domainFactory.getShowProjectData(projectId)

    data class DomainData(val name: String?, val userListDatas: Set<UserListData>, val friendDatas: Map<String, UserListData>) : com.krystianwsul.checkme.loaders.DomainData()

    data class UserListData(val name: String, val email: String, val id: String) {

        init {
            check(name.isNotEmpty())
            check(email.isNotEmpty())
            check(id.isNotEmpty())
        }
    }
}
