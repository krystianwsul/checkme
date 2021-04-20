package com.krystianwsul.checkme.firebase.factories

import com.krystianwsul.checkme.firebase.loaders.FactoryProvider
import com.krystianwsul.checkme.firebase.loaders.ProjectLoader
import com.krystianwsul.checkme.utils.publishImmediate
import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.firebase.ChangeType
import com.krystianwsul.common.firebase.json.Parsable
import com.krystianwsul.common.firebase.models.Project
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
        protected val factoryProvider: FactoryProvider,
        domainDisposable: CompositeDisposable,
        protected val deviceDbInfo: () -> DeviceDbInfo,
) {

    private val projectManager = initialProjectEvent.projectManager

    var project: Project<T>
        private set

    protected abstract fun newProject(
            projectRecord: ProjectRecord<T>,
            userCustomTimeProvider: JsonTime.UserCustomTimeProvider,
    ): Project<T>

    val changeTypes: Observable<ChangeType>

    init {
        project = newProject(initialProjectEvent.projectRecord, initialProjectEvent.userCustomTimeProvider)

        val changeProjectChangeTypes = projectLoader.changeProjectEvents.map { (projectChangeType, changeProjectEvent) ->
            project = newProject(changeProjectEvent.projectRecord, changeProjectEvent.userCustomTimeProvider)

            projectChangeType
        }

        changeTypes = listOf(changeProjectChangeTypes).merge().publishImmediate(domainDisposable)
    }
}