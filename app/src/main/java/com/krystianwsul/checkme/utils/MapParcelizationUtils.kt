@file:Suppress("UNCHECKED_CAST")

package com.krystianwsul.checkme.utils

import android.os.Bundle
import android.os.Parcelable
import com.krystianwsul.common.utils.Parcelize

fun <T : Parcelable, U : Parcelable> Bundle.getMap(key: String) =
        getParcelable<MapHolder>(key)?.let { it.map as Map<T, U> }

fun <T : Parcelable, U : Parcelable> Bundle.putMap(key: String, map: Map<T, U>) =
        putParcelable(key, MapHolder(map as Map<Parcelable, Parcelable>))

@Parcelize
private class MapHolder(val map: Map<Parcelable, Parcelable>) : Parcelable