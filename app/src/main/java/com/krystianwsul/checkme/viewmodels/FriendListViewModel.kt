package com.krystianwsul.checkme.viewmodels

import com.krystianwsul.checkme.domainmodel.extensions.getFriendListData
import com.krystianwsul.common.firebase.json.UserWrapper
import com.krystianwsul.common.utils.UserKey

class FriendListViewModel : DomainViewModel<FriendListViewModel.Data>() {

    override val domainListener = object : DomainListener<Data>() {

        override val domainResultFetcher = DomainResultFetcher.DomainFactoryData { it.getFriendListData() }
    }

    fun start() = internalStart()

    data class Data(val userListDatas: Set<UserListData>) : DomainData()

    data class UserListData(
            val name: String,
            val email: String,
            val id: UserKey,
            val photoUrl: String?,
            val userWrapper: UserWrapper
    ) {

        init {
            check(name.isNotEmpty())
            check(email.isNotEmpty())
        }
    }
}