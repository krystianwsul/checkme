package com.krystianwsul.checkme.firebase.snapshot

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.GenericTypeIndicator

class IndicatorSnapshot<T : Any>(override val key: String, private val value: T?) : Snapshot<T> {

    constructor(dataSnapshot: DataSnapshot, genericTypeIndicator: GenericTypeIndicator<T>) :
            this(dataSnapshot.key!!, dataSnapshot.getValue(genericTypeIndicator))

    override fun exists() = value != null

    override fun getValue() = value
}