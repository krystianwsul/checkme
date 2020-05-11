package com.krystianwsul.common.firebase.managers

import com.krystianwsul.common.firebase.records.RemoteRecord

abstract class ValueRecordManager<T : Any> : RecordManager {

    final override var isSaved = false
        protected set

    abstract val value: T

    abstract val records: Collection<RemoteRecord>
}