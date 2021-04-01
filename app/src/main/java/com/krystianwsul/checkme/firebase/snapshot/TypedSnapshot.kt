package com.krystianwsul.checkme.firebase.snapshot

import com.google.firebase.database.DataSnapshot
import kotlin.reflect.KClass

interface TypedSnapshot<T : Any> : Snapshot {

    fun getValue(): T? // todo remove param

    class Impl<T : Any>(private val dataSnapshot: DataSnapshot, private val kClass: KClass<T>) : TypedSnapshot<T> {

        override val key get() = dataSnapshot.key!!

        override fun exists() = dataSnapshot.exists()

        override fun getValue() = dataSnapshot.getValue(kClass.java)
    }
}