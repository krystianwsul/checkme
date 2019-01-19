package com.krystianwsul.checkme.viewmodels

import com.krystianwsul.checkme.domainmodel.DomainFactory

class FriendListViewModel : DomainViewModel<FriendListViewModel.Data>() {

    fun start() = internalStart()

    override fun getData(domainFactory: DomainFactory) = domainFactory.getFriendListData()

    data class Data(val userListDatas: MutableSet<UserListData>) : DomainData()

    data class UserListData(val name: String, val email: String, val id: String) {

        init {
            check(name.isNotEmpty())
            check(email.isNotEmpty())
            check(id.isNotEmpty())
        }
    }
}