package com.krystianwsul.checkme.domainmodel.extensions

import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.DomainFactory.Companion.syncOnDomain
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.common.firebase.UserData
import com.krystianwsul.common.firebase.json.UserWrapper

fun DomainFactory.addFriend(source: SaveService.Source, userWrapper: UserWrapper) = syncOnDomain {
    MyCrashlytics.log("DomainFactory.addFriend")
    check(!myUserFactory.isSaved)

    val userKey = UserData.getKey(userWrapper.userData.email)

    myUserFactory.user.addFriend(userKey)
    friendsFactory.addFriend(userKey, userWrapper)

    save(0, source)
}