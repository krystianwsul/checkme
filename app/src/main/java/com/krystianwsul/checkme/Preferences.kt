package com.krystianwsul.checkme

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import com.krystianwsul.checkme.utils.TaskKey
import com.krystianwsul.checkme.utils.time.ExactTimeStamp
import org.joda.time.LocalDateTime
import org.joda.time.format.DateTimeFormat
import java.lang.reflect.Type
import kotlin.properties.Delegates.observable
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

object Preferences {

    private const val LAST_TICK_KEY = "lastTick"
    private const val TICK_LOG = "tickLog"
    private const val TAB_KEY = "tab"
    private const val KEY_SHORTCUTS = "shortcuts2"

    private val sharedPreferences by lazy { MyApplication.sharedPreferences }

    private val gson by lazy {
        GsonBuilder().registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeTypeConverter()).create()!!
    }

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

    private var shortcutString: String by observable(sharedPreferences.getString(KEY_SHORTCUTS, "")!!) { _, _, newValue ->
        sharedPreferences.edit()
                .putString(KEY_SHORTCUTS, newValue)
                .apply()
    }

    private val shortcutTypeToken by lazy {
        object : TypeToken<Map<TaskKey, LocalDateTime>>() {}.type
    }

    var shortcuts: Map<TaskKey, LocalDateTime> by observable(gson.fromJson(shortcutString, shortcutTypeToken)
            ?: mapOf()) { _, _, newValue ->
        shortcutString = gson.toJson(newValue)
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

    private class LocalDateTimeTypeConverter : JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {

        companion object {

            private const val PATTERN = "yyyy-MM-dd HH:mm:ss"
        }

        override fun serialize(src: LocalDateTime, srcType: Type, context: JsonSerializationContext) = JsonPrimitive(src.toString(PATTERN)!!)

        override fun deserialize(json: JsonElement, type: Type, context: JsonDeserializationContext) = DateTimeFormat.forPattern(PATTERN).parseLocalDateTime(json.asString)!!
    }
}