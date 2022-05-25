package com.krystianwsul.common.firebase.managers

import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.records.project.PrivateOwnedProjectRecord

abstract class PrivateProjectManager : ValueRecordManager<List<PrivateOwnedProjectRecord>>() {

    override val records get() = value

    override val databasePrefix = DatabaseWrapper.PRIVATE_PROJECTS_KEY
}
