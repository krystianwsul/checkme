package com.krystianwsul.checkme.viewmodels

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.common.utils.UserKey

class FriendListViewModel : DomainViewModel<FriendListViewModel.Data>() {

    override val domainListener = object : DomainListener<Data>() {

        override fun getData(domainFactory: DomainFactory) = domainFactory.getFriendListData()
    }

    fun start() = internalStart()

    data class Data(val userListDatas: MutableSet<UserListData>) : DomainData()

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