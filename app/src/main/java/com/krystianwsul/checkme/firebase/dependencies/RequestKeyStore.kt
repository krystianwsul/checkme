package com.krystianwsul.checkme.firebase.dependencies

import com.jakewharton.rxrelay3.PublishRelay
import io.reactivex.rxjava3.core.Observable

class RequestKeyStore<REQUEST_KEY : Any, OUTPUT_KEY : Any> {

    private val customTimeEvents = PublishRelay.create<CustomTimeEvent<REQUEST_KEY, OUTPUT_KEY>>()

    val requestedOutputKeysObservable: Observable<Collection<Set<OUTPUT_KEY>>> =
        customTimeEvents.scan(CustomTimeAggregate<REQUEST_KEY, OUTPUT_KEY>()) { aggregate, customTimeEvent ->
            when (customTimeEvent) {
                is CustomTimeEvent.ProjectAdded<REQUEST_KEY, OUTPUT_KEY> -> {
                    val newProjectMap = aggregate.requestMap
                        .toMutableMap()
                        .also { it[customTimeEvent.requestKey] = customTimeEvent.outputKeys }

                    CustomTimeAggregate(newProjectMap)
                }
                is CustomTimeEvent.ProjectsRemoved<REQUEST_KEY, OUTPUT_KEY> -> {
                    val newProjectMap = aggregate.requestMap
                        .toMutableMap()
                        .also { map ->
                            customTimeEvent.requestKeys.forEach { map.remove(it) }
                        }

                CustomTimeAggregate(newProjectMap)
            }
        }
    }
            .map { it.output }

    fun addRequest(requestKey: REQUEST_KEY, outputKeys: Set<OUTPUT_KEY>) =
            customTimeEvents.accept(CustomTimeEvent.ProjectAdded(requestKey, outputKeys))

    fun onRequestsRemoved(requestKey: Set<REQUEST_KEY>) =
            customTimeEvents.accept(CustomTimeEvent.ProjectsRemoved(requestKey))

    private sealed class CustomTimeEvent<REQUEST_KEY : Any, OUTPUT_KEY : Any> {

        data class ProjectAdded<REQUEST_KEY : Any, OUTPUT_KEY : Any>(
                val requestKey: REQUEST_KEY,
                val outputKeys: Set<OUTPUT_KEY>,
        ) : CustomTimeEvent<REQUEST_KEY, OUTPUT_KEY>()

        data class ProjectsRemoved<REQUEST_KEY : Any, OUTPUT_KEY : Any>(
                val requestKeys: Set<REQUEST_KEY>,
        ) : CustomTimeEvent<REQUEST_KEY, OUTPUT_KEY>()
    }

    private data class CustomTimeAggregate<REQUEST_KEY : Any, OUTPUT_KEY : Any>(
            val requestMap: Map<REQUEST_KEY, Set<OUTPUT_KEY>> = mapOf(),
    ) {

        val output = requestMap.values
    }
}