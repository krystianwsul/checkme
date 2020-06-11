package com.krystianwsul.checkme.domainmodel.extensions

import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.common.firebase.json.UserWrapper
import com.krystianwsul.common.utils.UserKey

@Synchronized
fun DomainFactory.addFriend(
    source: SaveService.Source,
    userKey: UserKey,
    userWrapper: UserWrapper
) {
    MyCrashlytics.log("DomainFactory.addFriend")
    check(!myUserFactory.isSaved)

    myUserFactory.user.addFriend(userKey)
    friendsFactory.addFriend(userKey, userWrapper)

    save(0, source)
}