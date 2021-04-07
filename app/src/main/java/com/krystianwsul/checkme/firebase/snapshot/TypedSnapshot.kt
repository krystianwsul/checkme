package com.krystianwsul.checkme.firebase.snapshot

import com.google.firebase.database.DataSnapshot
import com.krystianwsul.common.firebase.json.Parsable
import kotlin.reflect.KClass

class TypedSnapshot<T : Parsable>(override val key: String, private val value: T?) : ValueSnapshot<T> {

    constructor(dataSnapshot: DataSnapshot, kClass: KClass<T>) :
            this(dataSnapshot.key!!, dataSnapshot.getValue(kClass.java))

    override fun exists() = value != null

    override fun getValue() = value
}