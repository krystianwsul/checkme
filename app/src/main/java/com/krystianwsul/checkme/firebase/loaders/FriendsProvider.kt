package com.krystianwsul.checkme.firebase.loaders

import com.krystianwsul.checkme.firebase.snapshot.UntypedSnapshot
import com.krystianwsul.common.utils.UserKey
import io.reactivex.rxjava3.core.Observable

interface FriendsProvider {

    val database: Database

    abstract class Database : ProjectProvider.Database() {

        abstract fun getUserObservable(userKey: UserKey): Observable<UntypedSnapshot>
    }
}