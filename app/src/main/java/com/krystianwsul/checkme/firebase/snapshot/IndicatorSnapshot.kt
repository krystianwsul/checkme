package com.krystianwsul.checkme.firebase.snapshot

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.GenericTypeIndicator

interface IndicatorSnapshot<T : Any> : Snapshot {

    fun getValue(): T?

    class Impl<T : Any>(
            private val dataSnapshot: DataSnapshot,
            private val genericTypeIndicator: GenericTypeIndicator<T>,
    ) : IndicatorSnapshot<T> {

        override val key get() = dataSnapshot.key!!

        override fun exists() = dataSnapshot.exists()

        override fun getValue() = dataSnapshot.getValue(genericTypeIndicator)
    }
}