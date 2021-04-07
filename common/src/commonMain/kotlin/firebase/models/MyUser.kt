package com.krystianwsul.common.firebase.models

import com.badoo.reaktive.subject.publish.PublishSubject
import com.krystianwsul.common.firebase.MyUserProperties
import com.krystianwsul.common.firebase.records.MyUserRecord
import com.krystianwsul.common.utils.UserKey


class MyUser(private val remoteMyUserRecord: MyUserRecord) :
        RootUser(remoteMyUserRecord),
        MyUserProperties by remoteMyUserRecord {

    override var photoUrl
        get() = super.photoUrl
        set(value) {
            remoteMyUserRecord.photoUrl = value
        }

    val friendChanges = PublishSubject<Unit>() // todo isSaved this won't be needed anymore

    override fun addFriend(userKey: UserKey) {
        super.addFriend(userKey)

        friendChanges.onNext(Unit)
    }

    override fun removeFriend(userKey: UserKey) {
        super.removeFriend(userKey)

        friendChanges.onNext(Unit)
    }
}
