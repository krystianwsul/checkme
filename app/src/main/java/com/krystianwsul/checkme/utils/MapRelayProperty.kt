package com.krystianwsul.checkme.utils

class MapRelayProperty<R, KEY : Any, VALUE : Any>(
        owner: R,
        initialValue: Map<KEY, VALUE>
) : RelayProperty<R, Map<KEY, VALUE>>(owner, initialValue) {

    operator fun set(key: KEY, value: VALUE) = mutate {
        toMutableMap().also { it[key] = value }
    }

    fun remove(key: KEY): VALUE? {
        var ret: VALUE? = null

        mutate {
            toMutableMap().apply {
                ret = remove(key)
            }
        }

        return ret
    }
}