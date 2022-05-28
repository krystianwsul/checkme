package com.krystianwsul.checkme.firebase.foreignProjects

import com.krystianwsul.checkme.firebase.factories.ForeignProjectFactory
import com.krystianwsul.checkme.firebase.factories.PrivateForeignProjectFactory
import com.krystianwsul.checkme.firebase.factories.SharedForeignProjectFactory
import com.krystianwsul.checkme.firebase.loaders.ProjectLoader
import com.krystianwsul.checkme.utils.MapRelayProperty
import com.krystianwsul.checkme.utils.publishImmediate
import com.krystianwsul.common.firebase.ChangeType
import com.krystianwsul.common.firebase.models.project.ForeignProject
import com.krystianwsul.common.firebase.records.project.ForeignProjectRecord
import com.krystianwsul.common.firebase.records.project.PrivateForeignProjectRecord
import com.krystianwsul.common.firebase.records.project.SharedForeignProjectRecord
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ProjectType
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.merge

class ForeignProjectsFactory(
    private val projectsLoader: ForeignProjectsLoader,
    private val domainDisposable: CompositeDisposable,
) {

    private fun newProjectFactory(
        projectLoader: ProjectLoader<*, *, ForeignProjectRecord<*>>,
        initialProjectEvent: ProjectLoader.InitialProjectEvent<ForeignProjectRecord<*>>,
    ) {
        val projectKey = initialProjectEvent.projectRecord.projectKey

        check(!projectFactories.containsKey(projectKey))

        projectFactoriesProperty[projectKey] = when (projectKey) {
            is ProjectKey.Private -> PrivateForeignProjectFactory(
                projectLoader as ProjectLoader<*, *, PrivateForeignProjectRecord>,
                initialProjectEvent as ProjectLoader.InitialProjectEvent<PrivateForeignProjectRecord>,
                domainDisposable,
            )
            is ProjectKey.Shared -> SharedForeignProjectFactory(
                projectLoader as ProjectLoader<*, *, SharedForeignProjectRecord>,
                initialProjectEvent as ProjectLoader.InitialProjectEvent<SharedForeignProjectRecord>,
                domainDisposable,
            )
        }
    }

    private val projectFactoriesProperty = MapRelayProperty<ProjectKey<*>, ForeignProjectFactory<*, *>>()

    private var projectFactories by projectFactoriesProperty

    val projects get() = projectFactories.mapValues { it.value.foreignProject }

    val remoteChanges: Observable<Unit>

    init {
        val initialRemoteChange = projectsLoader.initialProjectsEvent
            .doOnSuccess {
                it.initialProjectDatas.forEach { (projectLoader, initialProjectEvent) ->
                    newProjectFactory(projectLoader, initialProjectEvent)
                }
            }
            .map { }

        val addProjectRemoteChanges = projectsLoader.addProjectEvents
            .doOnNext { (changeType, addProjectEvent) ->
                check(changeType == ChangeType.REMOTE)

                newProjectFactory(addProjectEvent.projectLoader, addProjectEvent.initialProjectEvent)
            }
            .map { }

        val removeProjectRemoteChanges = projectsLoader.removeProjectEvents
            .doOnNext {
                it.projectKeys.forEach {
                    check(it is ProjectKey.Shared)

                    check(projectFactories.containsKey(it))

                    projectFactories.getValue(it)
                        .project
                        .clearableInvalidatableManager
                        .clear()

                    projectFactoriesProperty.remove(it)
                }
            }
            .map { }

        val projectFactoryRemoteChanges = projectFactoriesProperty.observable.switchMap {
            it.values
                .map { it.remoteChanges }
                .merge()
        }

        remoteChanges = listOf(
            initialRemoteChange.toObservable(),
            projectFactoryRemoteChanges,
            addProjectRemoteChanges,
            removeProjectRemoteChanges,
        ).merge().publishImmediate(domainDisposable)
    }

    fun save(values: MutableMap<String, Any?>) { // todo projectKey
        projectsLoader.save(values)
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : ProjectType> getProjectIfPresent(projectKey: ProjectKey<T>) =
        projects[projectKey] as ForeignProject<T>?

    fun <T : ProjectType> getProjectForce(projectKey: ProjectKey<T>) = getProjectIfPresent(projectKey)!!
}
