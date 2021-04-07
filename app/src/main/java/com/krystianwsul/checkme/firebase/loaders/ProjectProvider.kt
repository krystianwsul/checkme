package com.krystianwsul.checkme.firebase.loaders

import com.krystianwsul.checkme.firebase.managers.SnapshotRecordManager
import com.krystianwsul.checkme.firebase.snapshot.Snapshot
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.json.InstanceJson
import com.krystianwsul.common.firebase.json.Parsable
import com.krystianwsul.common.firebase.managers.RecordManager
import com.krystianwsul.common.firebase.records.ProjectRecord
import com.krystianwsul.common.utils.ProjectType
import io.reactivex.rxjava3.core.Observable

interface ProjectProvider {

    val database: Database

    abstract class Database : DatabaseWrapper() {

        abstract fun getRootInstanceObservable(
                taskFirebaseKey: String,
        ): Observable<Snapshot<Map<String, Map<String, InstanceJson>>>>
    }

    // U: Project JSON type
    interface ProjectManager<T : ProjectType, U : Parsable> :
            RecordManager,
            SnapshotRecordManager<ProjectRecord<T>, Snapshot<U>>
}