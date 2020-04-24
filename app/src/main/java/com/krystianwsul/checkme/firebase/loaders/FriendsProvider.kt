package com.krystianwsul.checkme.firebase.loaders

import com.krystianwsul.common.utils.UserKey
import io.reactivex.Observable

interface FriendsProvider {

    val database: Database

    abstract class Database : ProjectProvider.Database() {

        abstract fun getUserObservable(userKey: UserKey): Observable<Snapshot>
    }
}