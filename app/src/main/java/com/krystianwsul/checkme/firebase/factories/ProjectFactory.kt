package com.krystianwsul.checkme.firebase.factories

import com.krystianwsul.checkme.firebase.loaders.ProjectLoader
import com.krystianwsul.checkme.utils.publishImmediate
import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.firebase.json.Parsable
import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.firebase.models.cache.RootModelChangeManager
import com.krystianwsul.common.firebase.models.project.OwnedProject
import com.krystianwsul.common.firebase.records.project.ProjectRecord
import com.krystianwsul.common.time.JsonTime
import com.krystianwsul.common.utils.ProjectType
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.merge

@Suppress("LeakingThis")
abstract class ProjectFactory<T : ProjectType, U : Parsable, RECORD : ProjectRecord<T>>(
// U: Project JSON type
    projectLoader: ProjectLoader<T, U, RECORD>,
    initialProjectEvent: ProjectLoader.InitialProjectEvent<RECORD>,
    protected val shownFactory: Instance.ShownFactory,
    domainDisposable: CompositeDisposable,
    private val rootTaskProvider: OwnedProject.RootTaskProvider,
    private val rootModelChangeManager: RootModelChangeManager,
    protected val deviceDbInfo: () -> DeviceDbInfo,
) {

    var project: OwnedProject<T>
        private set

    protected abstract fun newProject(
        projectRecord: ProjectRecord<T>,
        userCustomTimeProvider: JsonTime.UserCustomTimeProvider,
        rootTaskProvider: OwnedProject.RootTaskProvider,
        rootModelChangeManager: RootModelChangeManager,
    ): OwnedProject<T>

    val remoteChanges: Observable<Unit>

    init {
        project = newProject(
            initialProjectEvent.projectRecord,
            initialProjectEvent.userCustomTimeProvider,
            rootTaskProvider,
            rootModelChangeManager,
        )

        rootModelChangeManager.invalidateProjects()

        val changeProjectChangeTypes = projectLoader.changeProjectEvents
            .doOnNext {
                project.clearableInvalidatableManager.clear()

                rootModelChangeManager.invalidateProjects()

                project =
                    newProject(it.projectRecord, it.userCustomTimeProvider, rootTaskProvider, rootModelChangeManager)
            }
            .map { }

        remoteChanges = listOf(changeProjectChangeTypes).merge().publishImmediate(domainDisposable)
    }
}