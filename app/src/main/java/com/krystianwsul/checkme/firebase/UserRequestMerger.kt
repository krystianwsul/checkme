package com.krystianwsul.checkme.firebase

import com.jakewharton.rxrelay3.PublishRelay
import com.krystianwsul.common.utils.UserKey
import io.reactivex.rxjava3.core.Observable

class UserRequestMerger<REQUEST_KEY : Any> {

    private val customTimeEvents = PublishRelay.create<CustomTimeEvent<REQUEST_KEY>>()

    val requestedUserKeysObservable: Observable<Set<UserKey>> = customTimeEvents.scan(CustomTimeAggregate<REQUEST_KEY>()) { aggregate, customTimeEvent ->
        when (customTimeEvent) {
            is CustomTimeEvent.ProjectAdded<REQUEST_KEY> -> {
                val newProjectMap = aggregate.requestMap
                        .toMutableMap()
                        .also { it[customTimeEvent.requestKey] = customTimeEvent.userKeys }

                CustomTimeAggregate(newProjectMap)
            }
            is CustomTimeEvent.ProjectsRemoved<REQUEST_KEY> -> {
                val newProjectMap = aggregate.requestMap
                        .toMutableMap()
                        .also { map ->
                            customTimeEvent.requestKeys.forEach { map.remove(it) }
                        }

                CustomTimeAggregate(newProjectMap)
            }
        }
    }.map { it.output }

    fun requestCustomTimeUsers(requestKey: REQUEST_KEY, userKeys: Set<UserKey>) =
            customTimeEvents.accept(CustomTimeEvent.ProjectAdded(requestKey, userKeys))

    fun onRequestsRemoved(requestKey: Set<REQUEST_KEY>) =
            customTimeEvents.accept(CustomTimeEvent.ProjectsRemoved(requestKey))

    private sealed class CustomTimeEvent<REQUEST_KEY : Any> {

        data class ProjectAdded<REQUEST_KEY : Any>(
                val requestKey: REQUEST_KEY,
                val userKeys: Set<UserKey>,
        ) : CustomTimeEvent<REQUEST_KEY>()

        data class ProjectsRemoved<REQUEST_KEY : Any>(
                val requestKeys: Set<REQUEST_KEY>,
        ) : CustomTimeEvent<REQUEST_KEY>()
    }

    private data class CustomTimeAggregate<REQUEST_KEY : Any>(
            val requestMap: Map<REQUEST_KEY, Set<UserKey>> = mapOf(),
    ) {

        val output = requestMap.values
                .flatten()
                .toSet()
    }
}