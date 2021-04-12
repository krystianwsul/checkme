package com.krystianwsul.checkme.firebase.snapshot

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.GenericTypeIndicator
import com.krystianwsul.common.firebase.json.Parsable
import kotlin.reflect.KClass

class Snapshot<T : Any>(val key: String, val value: T?) {

    companion object {

        fun <T : Parsable> fromParsable(dataSnapshot: DataSnapshot, kClass: KClass<T>) = Snapshot(dataSnapshot.key!!, dataSnapshot.getValue(kClass.java))

        fun <T : Any> fromTypeIndicator(dataSnapshot: DataSnapshot, genericTypeIndicator: GenericTypeIndicator<T>) =
                Snapshot(dataSnapshot.key!!, dataSnapshot.getValue(genericTypeIndicator))
    }

    val exists get() = value != null
}