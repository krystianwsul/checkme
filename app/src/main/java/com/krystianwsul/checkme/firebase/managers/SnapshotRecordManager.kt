package com.krystianwsul.checkme.firebase.managers

import com.krystianwsul.checkme.firebase.snapshot.Snapshot
import com.krystianwsul.common.firebase.ChangeWrapper

interface SnapshotRecordManager<T : Any, U : Snapshot<*>> {

    fun set(snapshot: U): ChangeWrapper<out T>?
}