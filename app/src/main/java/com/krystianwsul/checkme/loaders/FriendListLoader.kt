package com.krystianwsul.checkme.loaders

import android.content.Context

import com.krystianwsul.checkme.domainmodel.DomainFactory

import junit.framework.Assert

class FriendListLoader(context: Context) : DomainLoader<FriendListLoader.Data>(context, DomainLoader.FirebaseLevel.FRIEND) {

    override val name = "UserListLoader"

    override fun loadDomain(domainFactory: DomainFactory) = domainFactory.friendListData

    data class Data(val userListDatas: Set<UserListData>) : DomainLoader.Data()

    data class UserListData(val name: String, val email: String, val id: String) {

        init {
            Assert.assertTrue(name.isNotEmpty())
            Assert.assertTrue(email.isNotEmpty())
            Assert.assertTrue(id.isNotEmpty())
        }
    }
}
