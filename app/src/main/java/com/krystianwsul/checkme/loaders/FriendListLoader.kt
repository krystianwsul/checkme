package com.krystianwsul.checkme.loaders

import android.content.Context
import com.krystianwsul.checkme.domainmodel.DomainFactory

class FriendListLoader(context: Context) : DomainLoader<FriendListLoader.DomainData>(context, FirebaseLevel.FRIEND) {

    override val name = "UserListLoader"

    override fun loadDomain(domainFactory: DomainFactory) = domainFactory.friendListData

    data class DomainData(val userListDatas: Set<UserListData>) : com.krystianwsul.checkme.loaders.DomainData()

    data class UserListData(val name: String, val email: String, val id: String) {

        init {
            check(name.isNotEmpty())
            check(email.isNotEmpty())
            check(id.isNotEmpty())
        }
    }
}
