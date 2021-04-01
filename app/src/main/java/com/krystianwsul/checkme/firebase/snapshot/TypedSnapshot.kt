package com.krystianwsul.checkme.firebase.snapshot

import com.google.firebase.database.DataSnapshot

interface TypedSnapshot<T : Any> : Snapshot {

    class Impl<T : Any>(private val dataSnapshot: DataSnapshot) : TypedSnapshot<T> {

        override val key get() = dataSnapshot.key!!

        override fun exists() = dataSnapshot.exists()

        override fun <T> getValue(valueType: Class<T>) = dataSnapshot.getValue(valueType)
    }
}