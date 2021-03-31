package com.krystianwsul.checkme.firebase.loaders.snapshot

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.GenericTypeIndicator

interface Snapshot {

    val key: String

    val children: Iterable<Snapshot>

    fun exists(): Boolean

    fun <T> getValue(valueType: Class<T>): T?

    fun <T> getValue(genericTypeIndicator: GenericTypeIndicator<T>): T?

    class Impl(private val dataSnapshot: DataSnapshot) : Snapshot {

        override val key get() = dataSnapshot.key!!

        override val children get() = dataSnapshot.children.map { Impl(it) }

        override fun exists() = dataSnapshot.exists()

        override fun <T> getValue(valueType: Class<T>) = dataSnapshot.getValue(valueType)

        override fun <T> getValue(genericTypeIndicator: GenericTypeIndicator<T>) = dataSnapshot.getValue(genericTypeIndicator)
    }
}