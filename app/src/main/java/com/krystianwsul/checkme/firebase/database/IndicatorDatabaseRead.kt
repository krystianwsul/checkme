package com.krystianwsul.checkme.firebase.database

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.GenericTypeIndicator
import com.google.firebase.database.Query
import com.krystianwsul.checkme.firebase.Converter
import com.krystianwsul.checkme.firebase.snapshot.Snapshot
import com.krystianwsul.checkme.viewmodels.NullableWrapper
import io.reactivex.rxjava3.core.Observable

abstract class IndicatorDatabaseRead<DATA : Any> : DatabaseRead<DATA>() {

    override fun firebaseToSnapshot(dataSnapshot: DataSnapshot) =
        Snapshot.fromTypeIndicator(dataSnapshot, object : GenericTypeIndicator<DATA>() {})

    protected fun Query.indicatorSnapshotChanges(): Observable<Snapshot<DATA>> = cache(
        Converter(
            { Snapshot(path.back.asString(), it.value) },
            { NullableWrapper(it.value) },
        ),
    )
}