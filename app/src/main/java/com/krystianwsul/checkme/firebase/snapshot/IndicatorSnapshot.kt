package com.krystianwsul.checkme.firebase.snapshot

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.GenericTypeIndicator

interface IndicatorSnapshot<T : Any> : ValueSnapshot<T> {

    class Impl<T : Any>(
            private val dataSnapshot: DataSnapshot,
            genericTypeIndicator: GenericTypeIndicator<T>,
    ) : IndicatorSnapshot<T> {

        private val value = dataSnapshot.getValue(genericTypeIndicator)

        override val key get() = dataSnapshot.key!!

        override fun exists() = value != null

        override fun getValue() = value
    }

    class Wrapper<T : Any>(override val key: String, private val value: T?) : IndicatorSnapshot<T> {

        override fun exists() = value != null

        override fun getValue() = value
    }
}