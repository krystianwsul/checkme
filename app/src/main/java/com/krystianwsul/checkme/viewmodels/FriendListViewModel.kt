package com.krystianwsul.checkme.viewmodels

class FriendListViewModel : DomainViewModel<FriendListViewModel.Data>() {

    fun start() = internalStart(FirebaseLevel.FRIEND)

    override fun getData() = kotlinDomainFactory.getFriendListData()

    data class Data(val userListDatas: Set<UserListData>) : DomainData()

    data class UserListData(val name: String, val email: String, val id: String) {

        init {
            check(name.isNotEmpty())
            check(email.isNotEmpty())
            check(id.isNotEmpty())
        }
    }
}