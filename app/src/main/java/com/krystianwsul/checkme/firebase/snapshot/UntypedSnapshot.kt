package com.krystianwsul.checkme.firebase.snapshot

import com.google.firebase.database.DataSnapshot

interface UntypedSnapshot : ValueSnapshot {

    val children: Iterable<UntypedSnapshot>

    class Impl(private val dataSnapshot: DataSnapshot) : UntypedSnapshot {

        override val key get() = dataSnapshot.key!!

        override val children get() = dataSnapshot.children.map(::Impl)

        override fun exists() = dataSnapshot.exists()

        override fun <T> getValue(valueType: Class<T>) = dataSnapshot.getValue(valueType)
    }
}