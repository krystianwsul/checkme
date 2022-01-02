package com.krystianwsul.common.firebase.managers

import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.records.project.PrivateProjectRecord

abstract class PrivateProjectManager : ValueRecordManager<List<PrivateProjectRecord>>() {

    override val records get() = value

    override val databasePrefix = DatabaseWrapper.PRIVATE_PROJECTS_KEY
}
