package com.krystianwsul.checkme.firebase.database

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.GenericTypeIndicator
import com.google.firebase.database.Query
import com.krystianwsul.checkme.firebase.Converter
import com.krystianwsul.checkme.firebase.snapshot.Snapshot
import com.krystianwsul.checkme.viewmodels.NullableWrapper

abstract class IndicatorDatabaseRead<DATA : Any> : DatabaseRead<DATA>() {

    protected abstract val genericTypeIndicator: GenericTypeIndicator<DATA>

    override fun firebaseToSnapshot(dataSnapshot: DataSnapshot) =
        Snapshot.fromTypeIndicator(dataSnapshot, genericTypeIndicator)

    override fun Query.toSnapshot() = cache(
        Converter(
            { Snapshot(path.back.asString(), it.value) },
            { NullableWrapper(it.value) },
            { NullableWrapper(it.value?.let(::deepCopy)) },
        ),
    )

    protected abstract fun deepCopy(value: DATA): DATA
}