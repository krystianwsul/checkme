package com.krystianwsul.checkme.firebase.loaders

import com.krystianwsul.checkme.firebase.loaders.snapshot.UntypedSnapshot
import com.krystianwsul.checkme.firebase.managers.SnapshotRecordManager
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.managers.RecordManager
import com.krystianwsul.common.firebase.records.ProjectRecord
import com.krystianwsul.common.utils.ProjectType
import io.reactivex.rxjava3.core.Observable

interface ProjectProvider {

    val database: Database

    abstract class Database : DatabaseWrapper() {

        abstract fun getRootInstanceObservable(taskFirebaseKey: String): Observable<UntypedSnapshot>
    }

    interface ProjectManager<T : ProjectType> : RecordManager, SnapshotRecordManager<ProjectRecord<T>>
}