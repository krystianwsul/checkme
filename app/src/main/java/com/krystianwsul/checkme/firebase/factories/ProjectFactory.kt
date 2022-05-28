package com.krystianwsul.checkme.firebase.factories

import com.krystianwsul.checkme.firebase.loaders.ProjectLoader
import com.krystianwsul.checkme.utils.publishImmediate
import com.krystianwsul.common.firebase.json.Parsable
import com.krystianwsul.common.firebase.models.project.Project
import com.krystianwsul.common.firebase.records.project.ProjectRecord
import com.krystianwsul.common.time.JsonTime
import com.krystianwsul.common.utils.ProjectType
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable

abstract class ProjectFactory<TYPE : ProjectType, PARSABLE : Parsable, RECORD : ProjectRecord<TYPE>>(
    private val projectLoader: ProjectLoader<TYPE, PARSABLE, RECORD>,
    private val initialProjectEvent: ProjectLoader.InitialProjectEvent<RECORD>,
    private val domainDisposable: CompositeDisposable,
) {

    lateinit var project: Project<TYPE>
        private set

    lateinit var remoteChanges: Observable<Unit>
        private set

    // this is a separate function, because it must be called after the subclass initializers finish
    protected fun init() {
        project = newProject(initialProjectEvent.projectRecord, initialProjectEvent.userCustomTimeProvider)
        afterProjectChanged()

        remoteChanges = projectLoader.changeProjectEvents
            .doOnNext {
                project.clearableInvalidatableManager.clear()

                project = newProject(it.projectRecord, it.userCustomTimeProvider)
                afterProjectChanged()
            }
            .map { }
            .publishImmediate(domainDisposable)
    }

    protected abstract fun newProject(
        projectRecord: RECORD,
        userCustomTimeProvider: JsonTime.UserCustomTimeProvider,
    ): Project<TYPE>

    protected abstract fun afterProjectChanged()
}