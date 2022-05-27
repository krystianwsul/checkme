package com.krystianwsul.checkme.firebase.loaders

import com.krystianwsul.checkme.firebase.snapshot.Snapshot
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.json.Parsable
import com.krystianwsul.common.firebase.managers.RecordManager
import com.krystianwsul.common.firebase.records.project.ProjectRecord
import com.krystianwsul.common.utils.ProjectType

interface ProjectProvider {

    val database: DatabaseWrapper

    // U: Project JSON type
    interface ProjectManager<T : ProjectType, U : Parsable, RECORD : ProjectRecord<T>> : RecordManager {

        fun set(snapshot: Snapshot<U>): RECORD?
    }
}