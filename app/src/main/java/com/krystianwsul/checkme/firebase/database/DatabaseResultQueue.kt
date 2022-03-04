package com.krystianwsul.checkme.firebase.database

import com.krystianwsul.checkme.domainmodel.observeOnDomain
import com.krystianwsul.checkme.firebase.snapshot.Snapshot
import io.reactivex.rxjava3.core.Single

object DatabaseResultQueue {

    fun <T : Any> enqueueSnapshot(databaseRead: DatabaseRead<T>, snapshot: Snapshot<T>): Single<Snapshot<T>> {
        return Single.just(snapshot).observeOnDomain(databaseRead.priority)
    }
}