package com.krystianwsul.checkme.domainmodel.extensions

import androidx.annotation.CheckResult
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.DomainFactory.Companion.scheduleOnDomain
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.common.firebase.UserData
import com.krystianwsul.common.firebase.json.UserWrapper
import io.reactivex.rxjava3.core.Single

@CheckResult
fun DomainFactory.tryAddFriend(
        source: SaveService.Source,
        userWrapper: UserWrapper,
): Single<Boolean> = scheduleOnDomain {
    MyCrashlytics.log("DomainFactory.tryAddFriend")
    check(!myUserFactory.isSaved)

    val userKey = UserData.getKey(userWrapper.userData.email)

    if (myUserFactory.user.friends.contains(userKey)) {
        false
    } else {
        myUserFactory.user.addFriend(userKey)
        friendsFactory.addFriend(userKey, userWrapper)

        save(0, source)

        true
    }
}