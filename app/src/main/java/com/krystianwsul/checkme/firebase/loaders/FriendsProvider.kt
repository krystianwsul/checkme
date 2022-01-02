package com.krystianwsul.checkme.firebase.loaders

import com.krystianwsul.checkme.firebase.snapshot.Snapshot
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.json.users.UserWrapper
import com.krystianwsul.common.utils.UserKey
import io.reactivex.rxjava3.core.Observable

interface FriendsProvider {

    val database: Database

    abstract class Database : DatabaseWrapper() {

        abstract fun getUserObservable(userKey: UserKey): Observable<Snapshot<UserWrapper>>
    }
}