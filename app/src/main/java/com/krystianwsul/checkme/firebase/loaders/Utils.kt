package com.krystianwsul.checkme.firebase.loaders

import io.reactivex.rxjava3.core.Observable

fun <T : Any, U, V> Observable<T>.processChanges(
        keyGetter: (T) -> Set<U>,
        adder: (T, U) -> V,
        remover: ((V) -> Unit)? = null,
): Observable<MapChanges<T, U, V>> = scan(Pair<T?, MapChanges.Tmp<U, V>>(null, MapChanges.Tmp())) { (_, oldMapChanges), newData ->
    val oldMap = oldMapChanges.newMap
    val newKeys = keyGetter(newData)

    val removedKeys = oldMap.keys - newKeys
    val addedKeys = newKeys - oldMap.keys
    val unchangedKeys = newKeys - addedKeys

    val newMap = oldMap.toMutableMap().apply {
        addedKeys.forEach { put(it, adder(newData, it)) }
        removedKeys.forEach { remove(it) }
    }

    fun Set<U>.entries(map: Map<U, V>) = associate { it to map.getValue(it) }

    val removedEntries = removedKeys.entries(oldMap)

    remover?.let { removedEntries.values.forEach(it) }

    newData to MapChanges.Tmp(
            removedEntries,
            addedKeys.entries(newMap),
            unchangedKeys.entries(newMap),
            oldMap,
            newMap,
    )
}.skip(1).map { MapChanges(it.first!!, it.second) }