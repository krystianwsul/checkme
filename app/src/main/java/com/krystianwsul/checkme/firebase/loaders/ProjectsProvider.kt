package com.krystianwsul.checkme.firebase.loaders

import com.krystianwsul.checkme.firebase.snapshot.Snapshot
import com.krystianwsul.common.firebase.json.Parsable
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ProjectType
import io.reactivex.rxjava3.core.Observable

interface ProjectsProvider<TYPE : ProjectType, PARSABLE : Parsable> {

    fun getProjectObservable(projectKey: ProjectKey<out TYPE>): Observable<out Snapshot<out PARSABLE>>
}