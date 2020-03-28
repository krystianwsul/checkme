package com.krystianwsul.checkme.firebase.loaders

import io.reactivex.Observable

interface ProjectProvider {

    fun getRootInstanceObservable(taskFirebaseKey: String): Observable<FactoryProvider.Database.Snapshot>
}