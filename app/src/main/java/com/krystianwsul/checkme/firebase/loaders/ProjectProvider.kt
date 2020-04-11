package com.krystianwsul.checkme.firebase.loaders

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.records.ProjectRecord
import com.krystianwsul.common.utils.ProjectType
import io.reactivex.Observable

interface ProjectProvider {

    val database: Database

    abstract class Database : DatabaseWrapper() {

        abstract fun getRootInstanceObservable(taskFirebaseKey: String): Observable<Snapshot>
    }

    interface ProjectManager<T : ProjectType> {

        val isSaved: Boolean

        fun setProjectRecord(snapshot: Snapshot): ProjectRecord<T>

        fun save(extra: DomainFactory): Boolean
    }
}