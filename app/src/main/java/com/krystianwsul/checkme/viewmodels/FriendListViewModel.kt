package com.krystianwsul.checkme.viewmodels

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.loaders.FirebaseLevel

class FriendListViewModel : DomainViewModel<FriendListViewModel.Data>() {

    fun start() = internalStart(FirebaseLevel.FRIEND)

    override fun getData(domainFactory: DomainFactory) = domainFactory.friendListData

    data class Data(val userListDatas: Set<UserListData>) : DomainData()

    data class UserListData(val name: String, val email: String, val id: String) {

        init {
            check(name.isNotEmpty())
            check(email.isNotEmpty())
            check(id.isNotEmpty())
        }
    }
}