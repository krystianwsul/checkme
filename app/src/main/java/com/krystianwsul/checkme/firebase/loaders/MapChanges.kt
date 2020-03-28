package com.krystianwsul.checkme.firebase.loaders

@Suppress("unused")
class MapChanges<T, U>(
        val removedEntries: Map<T, U> = mapOf(),
        val addedEntries: Map<T, U> = mapOf(),
        val unchangedEntries: Map<T, U> = mapOf(),
        val oldMap: Map<T, U> = mapOf(),
        val newMap: Map<T, U> = mapOf()
)