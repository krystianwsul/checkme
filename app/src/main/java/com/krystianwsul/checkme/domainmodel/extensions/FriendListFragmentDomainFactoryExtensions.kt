package com.krystianwsul.checkme.domainmodel.extensions

import androidx.annotation.CheckResult
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.DomainListenerManager
import com.krystianwsul.checkme.domainmodel.DomainUpdater
import com.krystianwsul.checkme.viewmodels.FriendListViewModel
import com.krystianwsul.common.firebase.DomainThreadChecker
import com.krystianwsul.common.firebase.json.UserWrapper
import com.krystianwsul.common.utils.UserKey

fun DomainFactory.getFriendListData(): FriendListViewModel.Data {
    MyCrashlytics.log("DomainFactory.getFriendListData")

    DomainThreadChecker.instance.requireDomainThread()

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

@CheckResult
fun DomainUpdater.removeFriends(
        notificationType: DomainListenerManager.NotificationType,
        keys: Set<UserKey>,
) = updateDomainCompletable {
    MyCrashlytics.log("DomainFactory.removeFriends")

    keys.forEach { myUserFactory.user.removeFriend(it) }

    DomainUpdater.Params(notificationType = notificationType)
}

@CheckResult
fun DomainUpdater.addFriends(
        notificationType: DomainListenerManager.NotificationType,
        userMap: Map<UserKey, UserWrapper>,
) = updateDomainCompletable {
    MyCrashlytics.log("DomainFactory.addFriends")

    userMap.forEach {
        myUserFactory.user.addFriend(it.key)
        friendsFactory.addFriend(it.key, it.value)
    }

    DomainUpdater.Params(notificationType = notificationType)
}