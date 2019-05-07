package com.krystianwsul.checkme

import com.krystianwsul.checkme.utils.time.ExactTimeStamp
import kotlin.properties.Delegates.observable
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

object Preferences {

    private const val LAST_TICK_KEY = "lastTick"
    private const val TICK_LOG = "tickLog"
    private const val TAB_KEY = "tab"
    private const val KEY_SHORTCUTS = "shortcuts"

    private val sharedPreferences by lazy { MyApplication.sharedPreferences }

    var lastTick
        get() = sharedPreferences.getLong(LAST_TICK_KEY, -1)
        set(value) = sharedPreferences.edit()
                .putLong(LAST_TICK_KEY, value)
                .apply()

    var tickLog by ReadWriteStrPref(TICK_LOG)

    var tab by observable(sharedPreferences.getInt(TAB_KEY, 0)) { _, _, newValue ->
        sharedPreferences.edit()
                .putInt(TAB_KEY, newValue)
                .apply()
    }

    var shortcuts: Set<String> by observable(sharedPreferences.getStringSet(KEY_SHORTCUTS, setOf())!!) { _, _, newValue ->
        sharedPreferences.edit()
                .putStringSet(KEY_SHORTCUTS, newValue)
                .apply()
    }

    fun logLineDate(line: String) {
        logLine("")
        logLine(ExactTimeStamp.now.date.toString())
        logLine(ExactTimeStamp.now.hourMilli.toString() + " " + line)
    }

    fun logLineHour(line: String) = logLine(ExactTimeStamp.now.hourMilli.toString() + " " + line)

    private fun logLine(line: String) {
        MyCrashlytics.log("Preferences.logLine: $line")

        tickLog = tickLog.split('\n')
                .take(100)
                .toMutableList()
                .apply { add(0, line) }
                .joinToString("\n")
    }

    open class ReadOnlyStrPref(protected val key: String) : ReadOnlyProperty<Any, String> {

        final override fun getValue(thisRef: Any, property: KProperty<*>): String = sharedPreferences.getString(key, "")!!
    }

    class ReadWriteStrPref(key: String) : ReadOnlyStrPref(key), ReadWriteProperty<Any, String> {

        override fun setValue(thisRef: Any, property: KProperty<*>, value: String) = sharedPreferences.edit()
                .putString(key, value)
                .apply()
    }
}