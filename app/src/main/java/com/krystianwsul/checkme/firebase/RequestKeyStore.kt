package com.krystianwsul.checkme.firebase

import com.jakewharton.rxrelay3.PublishRelay
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.kotlin.combineLatest

class RequestKeyStore<REQUEST_KEY : Any, OUTPUT_KEY : Any> {

    companion object {

        fun <REQUEST1 : Any, REQUEST2 : Any, OUTPUT : Any> merge(
                store1: RequestKeyStore<REQUEST1, OUTPUT>,
                store2: RequestKeyStore<REQUEST2, OUTPUT>,
        ): Observable<Set<OUTPUT>> {
            return listOf(
                    store1,
                    store2,
            ).map { it.requestedOutputKeysObservable }
                    .combineLatest { it.flatten().toSet() }
                    .skip(1) // first event is just the initial empty sets from both
        }
    }

    private val customTimeEvents = PublishRelay.create<CustomTimeEvent<REQUEST_KEY, OUTPUT_KEY>>()

    val requestedOutputKeysObservable: Observable<Set<OUTPUT_KEY>> = customTimeEvents.scan(CustomTimeAggregate<REQUEST_KEY, OUTPUT_KEY>()) { aggregate, customTimeEvent ->
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
                            customTimeEvent.requestKeys.forEach {
                                check(map.containsKey(it))

                                map.remove(it)
                            }
                        }

                CustomTimeAggregate(newProjectMap)
            }
        }
    }
            .map { it.output }
            .distinctUntilChanged() // this initially emits an empty set.  It's necessary-ish for merging

    fun requestCustomTimeUsers(requestKey: REQUEST_KEY, userKeys: Set<OUTPUT_KEY>) =
            customTimeEvents.accept(CustomTimeEvent.ProjectAdded(requestKey, userKeys))

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
                .flatten()
                .toSet()
    }
}