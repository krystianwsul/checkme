package com.krystianwsul.checkme.firebase.managers

import com.krystianwsul.checkme.firebase.loaders.snapshot.UntypedSnapshot
import com.krystianwsul.common.firebase.ChangeWrapper

interface SnapshotRecordManager<T : Any> {

    fun set(snapshot: UntypedSnapshot): ChangeWrapper<out T>?
}