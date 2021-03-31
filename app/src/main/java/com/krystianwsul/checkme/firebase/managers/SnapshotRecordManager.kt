package com.krystianwsul.checkme.firebase.managers

import com.krystianwsul.checkme.firebase.loaders.snapshot.Snapshot
import com.krystianwsul.common.firebase.ChangeWrapper

interface SnapshotRecordManager<T : Any> {

    fun set(snapshot: Snapshot): ChangeWrapper<out T>?
}