package com.krystianwsul.checkme

import com.krystianwsul.checkme.utils.deserialize
import com.krystianwsul.checkme.utils.serialize
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.TaskKey
import org.joda.time.LocalDateTime
import kotlin.properties.Delegates.observable
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

object Preferences {

    private const val LAST_TICK_KEY = "lastTick"
    private const val TICK_LOG = "tickLog"
    private const val TAB_KEY = "tab"
    private const val KEY_SHORTCUTS = "shortcuts2"
    private const val KEY_TEMPORARY_NOTIFICATION_LOG = "temporaryNotificationLog"

    private val sharedPreferences by lazy { MyApplication.sharedPreferences }

    var lastTick
        get() = sharedPreferences.getLong(LAST_TICK_KEY, -1)
        set(value) = sharedPreferences.edit()
                .putLong(LAST_TICK_KEY, value)
                .apply()

    val tickLog = Logger(TICK_LOG)

    var tab by observable(sharedPreferences.getInt(TAB_KEY, 0)) { _, _, newValue ->
        sharedPreferences.edit()
                .putInt(TAB_KEY, newValue)
                .apply()
    }

    private var shortcutString: String by observable(sharedPreferences.getString(KEY_SHORTCUTS, "")!!) { _, _, newValue ->
        sharedPreferences.edit()
                .putString(KEY_SHORTCUTS, newValue)
                .apply()
    }

    var shortcuts: Map<TaskKey, LocalDateTime> by observable(deserialize<HashMap<TaskKey, LocalDateTime>>(shortcutString)
            ?: mapOf<TaskKey, LocalDateTime>()) { _, _, newValue ->
        shortcutString = serialize(HashMap(newValue))
    }

    val temporaryNotificationLog = Logger(KEY_TEMPORARY_NOTIFICATION_LOG)

    private open class ReadOnlyStrPref(protected val key: String) : ReadOnlyProperty<Any, String> {

        final override fun getValue(thisRef: Any, property: KProperty<*>): String = sharedPreferences.getString(key, "")!!
    }

    private class ReadWriteStrPref(key: String) : ReadOnlyStrPref(key), ReadWriteProperty<Any, String> {

        override fun setValue(thisRef: Any, property: KProperty<*>, value: String) = sharedPreferences.edit()
                .putString(key, value)
                .apply()
    }

    class Logger(key: String) {

        private var logString by ReadWriteStrPref(key)

        val log get() = logString

        fun logLineDate(line: String) {
            logLine("")
            logLine(ExactTimeStamp.now.date.toString())
            logLine(ExactTimeStamp.now.hourMilli.toString() + " " + line)
        }

        fun logLineHour(line: String) = logLine(ExactTimeStamp.now.hourMilli.toString() + " " + line)

        private fun logLine(line: String) {
            MyCrashlytics.log("Preferences.logLine: $line")

            logString = logString.split('\n')
                    .take(100)
                    .toMutableList()
                    .apply { add(0, line) }
                    .joinToString("\n")
        }
    }
}