package com.krystianwsul.checkme.gui

import com.krystianwsul.checkme.MyApplication
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

object Preferences {

    const val LAST_TICK_KEY = "lastTick"
    const val TICK_LOG = "tickLog"

    private val sharedPreferences by lazy { MyApplication.instance.sharedPreferences }

    var lastTick
        get() = sharedPreferences.getLong(LAST_TICK_KEY, -1)
        set(value) = sharedPreferences.edit()
                .putLong(LAST_TICK_KEY, value)
                .apply()

    var tickLog by ReadWriteStrPref(TICK_LOG)

    open class ReadOnlyStrPref(protected val key: String) : ReadOnlyProperty<Any, String> {

        final override fun getValue(thisRef: Any, property: KProperty<*>): String = sharedPreferences.getString(key, "")!!
    }

    class ReadWriteStrPref(key: String) : ReadOnlyStrPref(key), ReadWriteProperty<Any, String> {

        override fun setValue(thisRef: Any, property: KProperty<*>, value: String) {
            sharedPreferences.edit()
                    .putString(key, value)
                    .apply()
        }
    }
}