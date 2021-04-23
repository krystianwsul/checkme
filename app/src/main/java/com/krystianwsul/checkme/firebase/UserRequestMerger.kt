package com.krystianwsul.checkme.firebase

import com.jakewharton.rxrelay3.PublishRelay
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.UserKey
import io.reactivex.rxjava3.core.Observable

class UserRequestMerger() {

    private val customTimeEvents = PublishRelay.create<CustomTimeEvent>()

    val requestedUserKeysObservable: Observable<Set<UserKey>> = customTimeEvents.scan(CustomTimeAggregate()) { aggregate, customTimeEvent ->
        when (customTimeEvent) {
            is CustomTimeEvent.ProjectAdded -> {
                val newProjectMap = aggregate.projectMap
                        .toMutableMap()
                        .also { it[customTimeEvent.projectKey] = customTimeEvent.userKeys }

                CustomTimeAggregate(newProjectMap)
            }
            is CustomTimeEvent.ProjectsRemoved -> {
                val newProjectMap = aggregate.projectMap
                        .toMutableMap()
                        .also { map ->
                            customTimeEvent.projectKeys.forEach { map.remove(it) }
                        }

                CustomTimeAggregate(newProjectMap)
            }
        }
    }
            .skip(1)
            .map { it.output }

    fun requestCustomTimeUsers(projectKey: ProjectKey.Shared, userKeys: Set<UserKey>) =
            customTimeEvents.accept(CustomTimeEvent.ProjectAdded(projectKey, userKeys))

    fun onProjectsRemoved(projectKeys: Set<ProjectKey.Shared>) =
            customTimeEvents.accept(CustomTimeEvent.ProjectsRemoved(projectKeys))

    private sealed class CustomTimeEvent {

        data class ProjectAdded(val projectKey: ProjectKey.Shared, val userKeys: Set<UserKey>) : CustomTimeEvent()

        data class ProjectsRemoved(val projectKeys: Set<ProjectKey.Shared>) : CustomTimeEvent()
    }

    private data class CustomTimeAggregate(val projectMap: Map<ProjectKey.Shared, Set<UserKey>> = mapOf()) {

        val output = projectMap.values
                .flatten()
                .toSet()
    }
}