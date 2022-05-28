package com.krystianwsul.checkme.firebase.factories

import com.krystianwsul.checkme.firebase.loaders.ProjectLoader
import com.krystianwsul.checkme.utils.publishImmediate
import com.krystianwsul.common.firebase.json.Parsable
import com.krystianwsul.common.firebase.models.cache.RootModelChangeManager
import com.krystianwsul.common.firebase.models.project.OwnedProject
import com.krystianwsul.common.firebase.records.project.ProjectRecord
import com.krystianwsul.common.time.JsonTime
import com.krystianwsul.common.utils.ProjectType
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable

abstract class ProjectFactory<TYPE : ProjectType, PARCELABLE : Parsable, RECORD : ProjectRecord<TYPE>>(
    private val projectLoader: ProjectLoader<TYPE, PARCELABLE, RECORD>,
    private val initialProjectEvent: ProjectLoader.InitialProjectEvent<RECORD>,
    private val domainDisposable: CompositeDisposable,
    protected val rootModelChangeManager: RootModelChangeManager,
) {

    lateinit var project: OwnedProject<TYPE>
        private set

    lateinit var remoteChanges: Observable<Unit>
        private set

    protected fun init() {
        project = newProject(initialProjectEvent.projectRecord, initialProjectEvent.userCustomTimeProvider)

        rootModelChangeManager.invalidateProjects()

        remoteChanges = projectLoader.changeProjectEvents
            .doOnNext {
                project.clearableInvalidatableManager.clear()

                rootModelChangeManager.invalidateProjects()

                project = newProject(it.projectRecord, it.userCustomTimeProvider)
            }
            .map { }
            .publishImmediate(domainDisposable)
    }

    protected abstract fun newProject(
        projectRecord: RECORD,
        userCustomTimeProvider: JsonTime.UserCustomTimeProvider,
    ): OwnedProject<TYPE>
}