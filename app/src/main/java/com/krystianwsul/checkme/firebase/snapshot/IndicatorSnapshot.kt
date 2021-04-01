package com.krystianwsul.checkme.firebase.snapshot

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.GenericTypeIndicator

interface IndicatorSnapshot : ValueSnapshot {

    fun <T> getValue(genericTypeIndicator: GenericTypeIndicator<T>): T?

    class Impl(private val dataSnapshot: DataSnapshot) : IndicatorSnapshot {

        override val key get() = dataSnapshot.key!!

        override fun exists() = dataSnapshot.exists()

        override fun <T> getValue(valueType: Class<T>) = dataSnapshot.getValue(valueType)

        override fun <T> getValue(genericTypeIndicator: GenericTypeIndicator<T>) =
                dataSnapshot.getValue(genericTypeIndicator)
    }
}