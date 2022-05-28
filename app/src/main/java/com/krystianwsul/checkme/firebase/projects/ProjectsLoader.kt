package com.krystianwsul.checkme.firebase.projects

import com.jakewharton.rxrelay3.ReplayRelay
import com.krystianwsul.checkme.firebase.UserCustomTimeProviderSource
import com.krystianwsul.checkme.firebase.loaders.DatabaseRx
import com.krystianwsul.checkme.firebase.loaders.ProjectLoader
import com.krystianwsul.checkme.firebase.loaders.ProjectsProvider
import com.krystianwsul.checkme.firebase.loaders.processChanges
import com.krystianwsul.checkme.firebase.snapshot.Snapshot
import com.krystianwsul.checkme.utils.zipSingle
import com.krystianwsul.common.firebase.ChangeType
import com.krystianwsul.common.firebase.ChangeWrapper
import com.krystianwsul.common.firebase.json.Parsable
import com.krystianwsul.common.firebase.records.project.ProjectRecord
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ProjectType
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.merge
import io.reactivex.rxjava3.kotlin.plusAssign

abstract class ProjectsLoader<TYPE : ProjectType, RECORD : ProjectRecord<TYPE>, PARSABLE : Parsable>(
    projectKeysObservable: Observable<out Set<ProjectKey<out TYPE>>>,
    private val projectsManager: ProjectsManager<TYPE, PARSABLE, RECORD>,
    private val domainDisposable: CompositeDisposable,
    private val projectsProvider: ProjectsProvider<TYPE, PARSABLE>,
    private val userCustomTimeProviderSource: UserCustomTimeProviderSource,
) {

    protected data class AddedProjectData<RECORD : ProjectRecord<*>>(val initialProjectRecord: RECORD)

    protected val addedProjectDatasRelay =
        ReplayRelay.create<ChangeWrapper<Map<ProjectKey<out TYPE>, AddedProjectData<RECORD>?>>>()

    init {
        projectKeysObservable.map {
            ChangeWrapper<Map<ProjectKey<out TYPE>, AddedProjectData<RECORD>?>>(
                ChangeType.REMOTE,
                it.associateWith { null },
            )
        }
            .subscribe(addedProjectDatasRelay)
            .addTo(domainDisposable)
    }

    private fun <T : Any> Observable<T>.replayImmediate() = replay().apply { domainDisposable += connect() }
    private fun <T : Any> Single<T>.cacheImmediate() = cache().apply { domainDisposable += subscribe() }

    private data class ProjectData<TYPE : ProjectType, RECORD : ProjectRecord<TYPE>, PARSABLE : Parsable>(
        val userChangeType: ChangeType,
        val projectKeys: Set<ProjectKey<out TYPE>>,
        val newMap: Map<ProjectKey<out TYPE>, ProjectEntry<RECORD, PARSABLE>>,
    )

    private data class ProjectEntry<RECORD : ProjectRecord<*>, PARSABLE : Parsable>(
        val userChangeType: ChangeType,
        val databaseRx: DatabaseRx<out Snapshot<out PARSABLE>>,
        val initialProjectRecord: RECORD?,
    )

    private val projectDatabaseRxObservable = addedProjectDatasRelay.distinctUntilChanged()
        .processChanges(
            { it.data.keys },
            { (changeType, addedProjectDatas), projectKey ->
                val addedProjectData = addedProjectDatas.getValue(projectKey)

                ProjectEntry(
                    changeType,
                    DatabaseRx(
                        domainDisposable,
                        projectsProvider.getProjectObservable(projectKey),
                    ),
                    addedProjectData?.initialProjectRecord,
                )
            },
            { it.databaseRx.disposable.dispose() },
        )
        .map { ProjectData(it.original.changeType, it.original.data.keys, it.newMap) }
        .replayImmediate()

    private data class LoaderData<TYPE : ProjectType, PARSABLE : Parsable, RECORD : ProjectRecord<TYPE>>(
        val userChangeType: ChangeType,
        val newLoaderMap: Map<ProjectKey<out TYPE>, ProjectLoader<TYPE, PARSABLE, RECORD>>,
        val addedLoaderEntries: Map<ProjectKey<out TYPE>, ProjectLoader<TYPE, PARSABLE, RECORD>>,
        val removedProjectKeys: Set<ProjectKey<out TYPE>>,
    )

    private val projectLoadersObservable = projectDatabaseRxObservable.processChanges(
        { it.projectKeys },
        { mapChanges, projectKey ->
            val projectEntry = mapChanges.newMap.getValue(projectKey)

            if (projectEntry.initialProjectRecord != null) {
                check(mapChanges.userChangeType == ChangeType.LOCAL)
            }

            ProjectLoader.Impl<TYPE, PARSABLE, RECORD>(
                projectKey,
                projectEntry.databaseRx.observable,
                domainDisposable,
                projectsManager,
                projectEntry.initialProjectRecord,
                userCustomTimeProviderSource,
                ::onProjectAddedOrUpdated,
            )
        }
    )
        .map {
            LoaderData(
                it.original.userChangeType,
                it.newMap,
                it.addedEntries,
                it.removedEntries.keys,
            )
        }
        .replayImmediate()

    val initialProjectsEvent = projectLoadersObservable.firstOrError()
        .flatMap {
            it.newLoaderMap
                .values
                .map { projectLoader ->
                    projectLoader.initialProjectEvent
                        .doOnSuccess { check(it.changeType == ChangeType.REMOTE) }
                        .map { projectLoader to it }
                }
                .zipSingle()
        }
        .map {
            check(it.all { it.second.changeType == ChangeType.REMOTE })

            InitialProjectsEvent(it.map {
                InitialProjectData(
                    it.first,
                    it.second.data
                )
            })
        }
        .cacheImmediate()

    val addProjectEvents = projectLoadersObservable.skip(1)
        .switchMap {
            it.addedLoaderEntries
                .values
                .map { projectLoader ->
                    projectLoader.initialProjectEvent
                        .map { it.newData(AddProjectEvent(projectLoader, it.data)) }
                        .toObservable()
                }
                .merge()
        }
        .replayImmediate()

    protected abstract fun onProjectAddedOrUpdated(record: RECORD)

    protected abstract fun onProjectsRemoved(projectKeys: Set<ProjectKey<out TYPE>>)

    val removeProjectEvents = projectLoadersObservable.filter { it.removedProjectKeys.isNotEmpty() }
        .map {
            check(it.userChangeType == ChangeType.REMOTE)

            RemoveProjectsEvent(it.removedProjectKeys)
        }
        .doOnNext {
            it.projectKeys.forEach(projectsManager::remove)

            onProjectsRemoved(it.projectKeys)
        }
        .replayImmediate()

    fun save(values: MutableMap<String, Any?>) = projectsManager.save(values)

    class InitialProjectsEvent<TYPE : ProjectType, PARSABLE : Parsable, RECORD : ProjectRecord<TYPE>>(
        val initialProjectDatas: List<InitialProjectData<TYPE, PARSABLE, RECORD>>,
    )

    data class InitialProjectData<TYPE : ProjectType, PARSABLE : Parsable, RECORD : ProjectRecord<TYPE>>(
        val projectLoader: ProjectLoader<TYPE, PARSABLE, RECORD>,
        val initialProjectEvent: ProjectLoader.InitialProjectEvent<TYPE, PARSABLE, RECORD>,
    )

    class AddProjectEvent<TYPE : ProjectType, PARSABLE : Parsable, RECORD : ProjectRecord<TYPE>>(
        val projectLoader: ProjectLoader<TYPE, PARSABLE, RECORD>,
        val initialProjectEvent: ProjectLoader.InitialProjectEvent<TYPE, PARSABLE, RECORD>,
    )

    class RemoveProjectsEvent<TYPE : ProjectType>(val projectKeys: Set<ProjectKey<out TYPE>>)
}