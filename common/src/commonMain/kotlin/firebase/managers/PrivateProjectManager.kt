package com.krystianwsul.common.firebase.managers

import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.records.PrivateProjectRecord

abstract class PrivateProjectManager : ValueRecordManager<List<PrivateProjectRecord>>() {

    abstract val databaseWrapper: DatabaseWrapper

    abstract val privateProjectRecords: List<PrivateProjectRecord>

    override val records get() = privateProjectRecords

    override val value get() = privateProjectRecords

    override val databasePrefix = DatabaseWrapper.PRIVATE_PROJECTS_KEY
}
