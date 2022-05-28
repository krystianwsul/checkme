package com.krystianwsul.checkme.utils

class MapRelayProperty<KEY : Any, VALUE : Any>(initialValue: Map<KEY, VALUE> = mapOf()) :
    NonNullRelayProperty<Map<KEY, VALUE>>(initialValue) {

    operator fun set(key: KEY, value: VALUE) = mutate {
        it.toMutableMap().also { it[key] = value }
    }

    fun remove(key: KEY): VALUE? {
        var ret: VALUE? = null

        mutate {
            it.toMutableMap().apply { ret = remove(key) }
        }

        return ret
    }
}