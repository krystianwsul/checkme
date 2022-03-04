package com.krystianwsul.checkme.firebase.database

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.Query
import com.google.firebase.database.core.Path
import com.krystianwsul.checkme.firebase.Converter
import com.krystianwsul.checkme.firebase.snapshot.Snapshot
import com.krystianwsul.checkme.viewmodels.NullableWrapper
import com.krystianwsul.common.firebase.json.Parsable
import io.reactivex.rxjava3.core.Observable
import kotlin.reflect.KClass

abstract class TypedDatabaseRead<DATA : Parsable> : DatabaseRead<DATA>() {

    protected abstract val kClass: KClass<DATA>

    private class SnapshotConverter<T : Parsable>(path: Path) : Converter<NullableWrapper<T>, Snapshot<T>>(
        { Snapshot(path.back.asString(), it.value) },
        { NullableWrapper(it.value) },
    )

    override fun firebaseToSnapshot(dataSnapshot: DataSnapshot) = Snapshot.fromParsable(dataSnapshot, kClass)

    protected fun Query.typedSnapshotChanges(): Observable<Snapshot<DATA>> = cache(SnapshotConverter(path))
}