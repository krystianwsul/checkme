package com.krystianwsul.checkme.domainmodel.extensions

import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.DomainFactory.Companion.syncOnDomain
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.viewmodels.FriendListViewModel
import com.krystianwsul.common.firebase.SchedulerType
import com.krystianwsul.common.firebase.SchedulerTypeHolder
import com.krystianwsul.common.firebase.json.UserWrapper
import com.krystianwsul.common.utils.UserKey

fun DomainFactory.getFriendListData(): FriendListViewModel.Data {
    MyCrashlytics.log("DomainFactory.getFriendListData")

    SchedulerTypeHolder.instance.requireScheduler(SchedulerType.DOMAIN)

    val friends = friendsFactory.friends

    val userListDatas = friends.map {
        FriendListViewModel.UserListData(
                it.name,
                it.email,
                it.userKey,
                it.photoUrl,
                it.userWrapper
        )
    }.toMutableSet()

    return FriendListViewModel.Data(userListDatas)
}

fun DomainFactory.removeFriends(source: SaveService.Source, keys: Set<UserKey>) = syncOnDomain {
    MyCrashlytics.log("DomainFactory.removeFriends")
    check(!friendsFactory.isSaved)

    keys.forEach { myUserFactory.user.removeFriend(it) }

    save(0, source)
}

fun DomainFactory.addFriends(source: SaveService.Source, userMap: Map<UserKey, UserWrapper>) = syncOnDomain {
    MyCrashlytics.log("DomainFactory.addFriends")
    check(!myUserFactory.isSaved)

    userMap.forEach {
        myUserFactory.user.addFriend(it.key)
        friendsFactory.addFriend(it.key, it.value)
    }

    save(0, source)
}