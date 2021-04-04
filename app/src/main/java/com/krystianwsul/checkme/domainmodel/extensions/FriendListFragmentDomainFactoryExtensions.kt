package com.krystianwsul.checkme.domainmodel.extensions

import androidx.annotation.CheckResult
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.DomainListenerManager
import com.krystianwsul.checkme.domainmodel.update.CompletableDomainUpdate
import com.krystianwsul.checkme.domainmodel.update.DomainUpdater
import com.krystianwsul.checkme.viewmodels.FriendListViewModel
import com.krystianwsul.common.firebase.DomainThreadChecker
import com.krystianwsul.common.firebase.json.UserWrapper
import com.krystianwsul.common.utils.UserKey
import io.reactivex.rxjava3.core.Completable

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
): Completable = CompletableDomainUpdate.create("removeFriends") {
    keys.forEach { myUserFactory.user.removeFriend(it) }

    DomainUpdater.Params(false, notificationType)
}.perform(this)

@CheckResult
fun DomainUpdater.addFriends(
        notificationType: DomainListenerManager.NotificationType,
        userMap: Map<UserKey, UserWrapper>,
): Completable = CompletableDomainUpdate.create("addFriends") {
    userMap.forEach {
        myUserFactory.user.addFriend(it.key)
        friendsFactory.addFriend(it.key, it.value)
    }

    DomainUpdater.Params(false, notificationType)
}.perform(this)