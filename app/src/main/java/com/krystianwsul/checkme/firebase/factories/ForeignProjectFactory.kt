package com.krystianwsul.checkme.firebase.factories

import com.krystianwsul.checkme.firebase.loaders.ProjectLoader
import com.krystianwsul.common.firebase.models.cache.RootModelChangeManager
import com.krystianwsul.common.firebase.models.project.ForeignProject
import com.krystianwsul.common.firebase.records.project.ForeignProjectRecord
import com.krystianwsul.common.time.JsonTime
import com.krystianwsul.common.utils.ProjectType
import io.reactivex.rxjava3.disposables.CompositeDisposable

abstract class ForeignProjectFactory<TYPE : ProjectType, RECORD : ForeignProjectRecord<TYPE>>(
    projectLoader: ProjectLoader<*, *, RECORD>,
    initialProjectEvent: ProjectLoader.InitialProjectEvent<RECORD>,
    domainDisposable: CompositeDisposable,
    protected val rootModelChangeManager: RootModelChangeManager,
) : ProjectFactory<TYPE, RECORD>(projectLoader, initialProjectEvent, domainDisposable) {

    abstract override fun newProject(
        projectRecord: RECORD,
        userCustomTimeProvider: JsonTime.UserCustomTimeProvider
    ): ForeignProject<TYPE>

    override fun afterProjectChanged() = rootModelChangeManager.invalidateProjects()
}