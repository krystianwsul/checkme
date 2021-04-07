package com.krystianwsul.checkme.firebase.snapshot

import com.google.firebase.database.DataSnapshot
import com.krystianwsul.common.firebase.json.Parsable
import kotlin.reflect.KClass

interface TypedSnapshot<T : Parsable> : ValueSnapshot<T> {

    class Impl<T : Parsable>(private val dataSnapshot: DataSnapshot, kClass: KClass<T>) : TypedSnapshot<T> {

        private val value = dataSnapshot.getValue(kClass.java)

        override val key get() = dataSnapshot.key!!

        override fun exists() = value != null

        override fun getValue() = value
    }

    class Wrapper<T : Parsable>(override val key: String, private val value: T?) : TypedSnapshot<T> {

        override fun exists() = value != null

        override fun getValue() = value
    }
}