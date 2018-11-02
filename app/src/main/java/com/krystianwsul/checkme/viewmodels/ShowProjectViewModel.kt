package com.krystianwsul.checkme.viewmodels

class ShowProjectViewModel : DomainViewModel<ShowProjectViewModel.Data>() {

    private var projectId: String? = null

    fun start(projectId: String?) {
        this.projectId = projectId

        internalStart(FirebaseLevel.FRIEND)
    }

    override fun getData() = kotlinDomainFactory.getShowProjectData(projectId)

    data class Data(
            val name: String?,
            val userListDatas: Set<UserListData>,
            val friendDatas: Map<String, UserListData>) : DomainData()

    data class UserListData(val name: String, val email: String, val id: String) {

        init {
            check(name.isNotEmpty())
            check(email.isNotEmpty())
            check(id.isNotEmpty())
        }
    }
}