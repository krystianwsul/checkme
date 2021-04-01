package com.krystianwsul.checkme.firebase.snapshot

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.GenericTypeIndicator

interface IndicatorSnapshot<T : Any> : Snapshot {

    fun <T> getValue(genericTypeIndicator: GenericTypeIndicator<T>): T?

    class Impl<T : Any>(private val dataSnapshot: DataSnapshot) : IndicatorSnapshot<T> {

        override val key get() = dataSnapshot.key!!

        override fun exists() = dataSnapshot.exists()

        override fun <T> getValue(genericTypeIndicator: GenericTypeIndicator<T>) =
                dataSnapshot.getValue(genericTypeIndicator)
    }
}