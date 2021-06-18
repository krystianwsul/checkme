package com.krystianwsul.checkme.viewmodels

import com.krystianwsul.checkme.domainmodel.extensions.getShowProjectData
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.UserKey

class ShowProjectViewModel : DomainViewModel<ShowProjectViewModel.Data>() {

    private var projectId: ProjectKey.Shared? = null

    override val domainListener = object : DomainListener<Data>() {

        override val domainResultFetcher = DomainResultFetcher.DomainFactoryData { it.getShowProjectData(projectId) }
    }

    fun start(projectId: ProjectKey.Shared?) {
        this.projectId = projectId

        internalStart()
    }

    data class Data(
            val name: String?,
            val userListDatas: Set<UserListData>,
            val friendDatas: Map<UserKey, UserListData>
    ) : DomainData()

    data class UserListData(
            val name: String,
            val email: String,
            val id: UserKey,
            val photoUrl: String?
    ) {

        init {
            check(name.isNotEmpty())
            check(email.isNotEmpty())
        }
    }
}