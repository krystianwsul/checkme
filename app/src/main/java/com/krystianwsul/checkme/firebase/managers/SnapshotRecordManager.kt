package com.krystianwsul.checkme.firebase.managers

import com.krystianwsul.checkme.firebase.snapshot.ValueSnapshot
import com.krystianwsul.common.firebase.ChangeWrapper

interface SnapshotRecordManager<T : Any, U : ValueSnapshot<*>> {

    fun set(snapshot: U): ChangeWrapper<out T>?
}