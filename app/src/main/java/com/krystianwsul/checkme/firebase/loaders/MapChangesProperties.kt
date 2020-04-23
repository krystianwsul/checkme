package com.krystianwsul.checkme.firebase.loaders

interface MapChangesProperties<T, U> {

    val removedEntries: Map<T, U>
    val addedEntries: Map<T, U>
    val unchangedEntries: Map<T, U>
    val oldMap: Map<T, U>
    val newMap: Map<T, U>
}