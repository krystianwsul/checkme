package com.krystianwsul.checkme.firebase.snapshot

import com.google.firebase.database.DataSnapshot
import kotlin.reflect.KClass

interface TypedSnapshot<T : Any> : ValueSnapshot<T> {

    class Impl<T : Any>(private val dataSnapshot: DataSnapshot, kClass: KClass<T>) : TypedSnapshot<T> {

        private val value = dataSnapshot.getValue(kClass.java)

        override val key get() = dataSnapshot.key!!

        override fun exists() = value != null

        override fun getValue() = value
    }
}