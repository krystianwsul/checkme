package com.krystianwsul.checkme.firebase.loaders

import com.google.firebase.database.GenericTypeIndicator
import com.krystianwsul.checkme.firebase.managers.AndroidRootInstanceManager
import com.krystianwsul.common.firebase.json.InstanceJson
import io.reactivex.Observable

fun <T, U, V> Observable<T>.processChanges(
        keyGetter: (T) -> Set<U>,
        adder: (T, U) -> V,
        remover: (V) -> Unit
): Observable<Pair<T, MapChanges<U, V>>> = scan(Pair<T?, MapChanges<U, V>>(null, MapChanges())) { (value, oldMapChanges), newData ->
    val oldMap = oldMapChanges.newMap
    val newKeys = keyGetter(newData)

    val removedKeys = oldMap.keys - newKeys
    val addedKeys = newKeys - oldMap.keys
    val unchangedKeys = newKeys - addedKeys

    val newMap = oldMap.toMutableMap().apply {
        addedKeys.forEach { put(it, adder(newData, it)) }
    }

    fun Set<U>.entries(map: Map<U, V>) = map { it to map.getValue(it) }.toMap()

    val removedEntries = removedKeys.entries(oldMap)

    removedEntries.values.forEach(remover)

    value to MapChanges(
            removedEntries,
            addedKeys.entries(newMap),
            unchangedKeys.entries(newMap),
            oldMap,
            newMap
    )
}.skip(1).map { Pair(it.first!!, it.second) }

fun <T, U> Observable<Set<T>>.processChanges(
        adder: (T) -> U,
        remover: (U) -> Unit
) = processChanges(
        { it },
        { _, key -> adder(key) },
        remover
)

fun <T, U, V> Observable<Map<T, U>>.processChanges(
        adder: (T, U) -> V,
        remover: (V) -> Unit
) = processChanges(
        { it.keys },
        { newData, key -> adder(key, newData.getValue(key)) },
        remover
)

private val typeToken = object : GenericTypeIndicator<Map<String, Map<String, InstanceJson>>>() {}

fun FactoryProvider.Database.Snapshot.toSnapshotInfos() = getValue(typeToken)?.map { (dateString, timeMap) ->
    timeMap.map { (timeString, instanceJson) ->
        AndroidRootInstanceManager.SnapshotInfo(dateString, timeString, instanceJson)
    }
}
        ?.flatten()
        ?: listOf()