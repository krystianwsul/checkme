package com.krystianwsul.checkme.domainmodel.extensions

import androidx.annotation.CheckResult
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainListenerManager
import com.krystianwsul.checkme.domainmodel.update.DomainUpdater
import com.krystianwsul.checkme.domainmodel.update.SingleDomainUpdate
import com.krystianwsul.common.firebase.UserData
import com.krystianwsul.common.firebase.json.UserWrapper
import io.reactivex.rxjava3.core.Single

@CheckResult
fun DomainUpdater.tryAddFriend(
        notificationType: DomainListenerManager.NotificationType,
        userWrapper: UserWrapper,
): Single<Boolean> = SingleDomainUpdate.create {
    MyCrashlytics.log("DomainFactory.tryAddFriend")

    val userKey = UserData.getKey(userWrapper.userData.email)

    if (myUserFactory.user.friends.contains(userKey)) {
        DomainUpdater.Result(false)
    } else {
        myUserFactory.user.addFriend(userKey)
        friendsFactory.addFriend(userKey, userWrapper)

        DomainUpdater.Result(true, false, notificationType)
    }
}.perform(this)