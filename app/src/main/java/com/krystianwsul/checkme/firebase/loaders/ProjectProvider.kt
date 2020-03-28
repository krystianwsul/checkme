package com.krystianwsul.checkme.firebase.loaders

import com.krystianwsul.common.firebase.DatabaseWrapper
import io.reactivex.Observable

abstract class ProjectProvider : DatabaseWrapper() {

    abstract fun getRootInstanceObservable(taskFirebaseKey: String): Observable<FactoryProvider.Database.Snapshot>
}