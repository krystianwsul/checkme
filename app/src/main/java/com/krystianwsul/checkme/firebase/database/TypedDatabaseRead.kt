package com.krystianwsul.checkme.firebase.database

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.Query
import com.google.firebase.database.core.Path
import com.krystianwsul.checkme.firebase.Converter
import com.krystianwsul.checkme.firebase.snapshot.Snapshot
import com.krystianwsul.checkme.viewmodels.NullableWrapper
import com.krystianwsul.common.firebase.json.DeepCopy
import kotlin.reflect.KClass

abstract class TypedDatabaseRead<DATA : DeepCopy<DATA>> : DatabaseRead<DATA>() {

    protected abstract val kClass: KClass<DATA>

    private class SnapshotConverter<T : DeepCopy<T>>(path: Path) : Converter<NullableWrapper<T>, Snapshot<T>>(
        { Snapshot(path.back.asString(), it.value) },
        { NullableWrapper(it.value) },
        { NullableWrapper(it.value?.deepCopy()) },
    )

    override fun firebaseToSnapshot(dataSnapshot: DataSnapshot) = Snapshot.fromParsable(dataSnapshot, kClass)

    override fun Query.toSnapshot() = cache(SnapshotConverter(path))
}