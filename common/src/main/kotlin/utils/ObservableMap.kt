package com.krystianwsul.common.utils

class ObservableMap<KEY, VALUE>(private val innerMap: MutableMap<KEY, VALUE>) :
    Map<KEY, VALUE> by innerMap {

    lateinit var callback: () -> Unit

    operator fun set(key: KEY, value: VALUE) {
        innerMap[key] = value

        callback()
    }

    fun remove(key: KEY): VALUE? {
        val ret = innerMap.remove(key)

        callback()

        return ret
    }
}

fun <KEY, VALUE> Map<KEY, VALUE>.toObservableMap() = ObservableMap(this.toMutableMap())