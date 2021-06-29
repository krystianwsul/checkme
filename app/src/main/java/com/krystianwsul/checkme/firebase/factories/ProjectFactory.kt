package com.krystianwsul.checkme.firebase.factories

import com.krystianwsul.checkme.firebase.loaders.ProjectLoader
import com.krystianwsul.checkme.utils.publishImmediate
import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.firebase.ChangeType
import com.krystianwsul.common.firebase.json.Parsable
import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.firebase.models.project.Project
import com.krystianwsul.common.firebase.records.project.ProjectRecord
import com.krystianwsul.common.time.JsonTime
import com.krystianwsul.common.utils.ProjectType
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.merge

@Suppress("LeakingThis")
abstract class ProjectFactory<T : ProjectType, U : Parsable>(
// U: Project JSON type
    projectLoader: ProjectLoader<T, U>,
    initialProjectEvent: ProjectLoader.InitialProjectEvent<T, U>,
    protected val shownFactory: Instance.ShownFactory,
    domainDisposable: CompositeDisposable,
    private val rootTaskProvider: Project.RootTaskProvider,
    protected val deviceDbInfo: () -> DeviceDbInfo,
) {

    private val projectManager = initialProjectEvent.projectManager

    var project: Project<T>
        private set

    protected abstract fun newProject(
        projectRecord: ProjectRecord<T>,
        userCustomTimeProvider: JsonTime.UserCustomTimeProvider,
        rootTaskProvider: Project.RootTaskProvider,
    ): Project<T>

    val changeTypes: Observable<ChangeType>

    init {
        project = newProject(
                initialProjectEvent.projectRecord,
                initialProjectEvent.userCustomTimeProvider,
                rootTaskProvider,
        )

        val changeProjectChangeTypes = projectLoader.changeProjectEvents.map {
            project.rootCacheCoordinator.clear()

            project = newProject(it.projectRecord, it.userCustomTimeProvider, rootTaskProvider)

            ChangeType.REMOTE
        }

        changeTypes = listOf(changeProjectChangeTypes).merge().publishImmediate(domainDisposable)
    }
}