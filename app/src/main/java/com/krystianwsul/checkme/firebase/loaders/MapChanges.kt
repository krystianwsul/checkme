package com.krystianwsul.checkme.firebase.loaders

class MapChanges<T, U, V>(
        val original: T,
        private val tmp: Tmp<U, V>
) : MapChangesProperties<U, V> by tmp {

    class Tmp<U, V>(
            override val removedEntries: Map<U, V> = mapOf(),
            override val addedEntries: Map<U, V> = mapOf(),
            override val unchangedEntries: Map<U, V> = mapOf(),
            override val oldMap: Map<U, V> = mapOf(),
            override val newMap: Map<U, V> = mapOf()
    ) : MapChangesProperties<U, V>
}