package com.krystianwsul.checkme.firebase.loaders

import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.records.ProjectRecord
import com.krystianwsul.common.utils.ProjectType
import io.reactivex.Observable

interface ProjectProvider {

    val database: Database

    abstract class Database : DatabaseWrapper() {

        abstract fun getRootInstanceObservable(taskFirebaseKey: String): Observable<FactoryProvider.Database.Snapshot>
    }

    interface ProjectManager<T : ProjectType> {

        fun setProjectRecord(snapshot: FactoryProvider.Database.Snapshot): ProjectRecord<T>
    }
}