package com.krystianwsul.checkme

import android.content.SharedPreferences
import androidx.core.content.edit
import com.akexorcist.localizationactivity.core.LanguageSetting
import com.jakewharton.rxrelay3.BehaviorRelay
import com.krystianwsul.checkme.gui.base.AbstractActivity
import com.krystianwsul.checkme.utils.NonNullRelayProperty
import com.krystianwsul.checkme.utils.deserialize
import com.krystianwsul.checkme.utils.ignore
import com.krystianwsul.checkme.utils.serialize
import com.krystianwsul.checkme.viewmodels.NullableWrapper
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.TaskKey
import com.soywiz.klock.DateTimeTz
import com.soywiz.klock.days
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import org.joda.time.LocalDateTime
import java.util.*
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
    private const val KEY_MAIN_TABS_LOG = "mainTabsLog"
    private const val TOKEN_KEY = "token"
    private const val KEY_SHOW_NOTIFICATIONS = "showNotifications"
    private const val KEY_NOTIFICATION_LEVEL = "notificationLevel"
    private const val KEY_ADD_DEFAULT_REMINDER = "addDefaultReminder"
    private const val KEY_SHOW_DELETED = "showDeleted"
    private const val KEY_SHOW_ASSIGNED_TO = "showAssignedTo"
    private const val KEY_TOOLTIP_SHOWN = "tooltipShown"
    private const val KEY_SHOW_PROJECTS = "showProjects"
    private const val KEY_LANGUAGE = "language"
    private const val KEY_VERSION_CODE = "versionCode"
    private const val KEY_INSTANCE_WARNING_SNOOZE = "instanceWarningSnooze"
    private const val KEY_PROJECT_ORDER = "projectOrder"
    private const val KEY_NOTIFICATION_LOG = "notificationLog"
    private const val KEY_PROJECT_FILTER = "projectFilter"

    private val sharedPreferences by lazy { MyApplication.sharedPreferences }

    var lastTick
        get() = sharedPreferences.getLong(LAST_TICK_KEY, -1)
        set(value) = sharedPreferences.edit { putLong(LAST_TICK_KEY, value) }

    val tickLog = Logger(TICK_LOG)

    private fun <T> preferenceObservable(
        initial: SharedPreferences.() -> T,
        write: SharedPreferences.Editor.(T) -> Unit,
    ) = observable(sharedPreferences.initial()) { _, _, newValue -> sharedPreferences.edit { write(newValue) } }

    var tab by preferenceObservable({ getInt(TAB_KEY, 0) }, { putInt(TAB_KEY, it) })

    private fun booleanObservable(key: String, defValue: Boolean) = preferenceObservable(
        { getBoolean(key, defValue) },
        { putBoolean(key, it) }
    )

    private fun intObservable(key: String, defValue: Int) = preferenceObservable(
        { getInt(key, defValue) },
        { putInt(key, it) }
    )

    var addDefaultReminder by booleanObservable(KEY_ADD_DEFAULT_REMINDER, true)

    private var shortcutString: String by observable(sharedPreferences.getString(KEY_SHORTCUTS, "")!!) { _, _, newValue ->
        sharedPreferences.edit { putString(KEY_SHORTCUTS, newValue) }
    }

    var shortcuts: Map<TaskKey, LocalDateTime> by observable(
        deserialize<HashMap<TaskKey, LocalDateTime>>(shortcutString) ?: mapOf()
    ) { _, _, newValue -> shortcutString = serialize(HashMap(newValue)) }

    val temporaryNotificationLog = Logger(KEY_TEMPORARY_NOTIFICATION_LOG)

    val tokenRelay =
        BehaviorRelay.createDefault(NullableWrapper(sharedPreferences.getString(TOKEN_KEY, null)))

    val mainTabsLog = Logger(KEY_MAIN_TABS_LOG, 10)

    val versionCode get() = sharedPreferences.getInt(KEY_VERSION_CODE, -1).takeIf { it != -1 }

    fun setVersionCode(versionCode: Int) = sharedPreferences.edit { putInt(KEY_VERSION_CODE, versionCode) }

    init {
        tokenRelay.distinctUntilChanged()
            .skip(1)
            .subscribe { sharedPreferences.edit { putString(TOKEN_KEY, it.value) } }
            .ignore()
    }

    var token: String?
        get() = tokenRelay.value!!.value
        set(value) = tokenRelay.accept(NullableWrapper(value))

    init {
        if (!sharedPreferences.contains(KEY_NOTIFICATION_LEVEL) && sharedPreferences.contains(KEY_SHOW_NOTIFICATIONS)) {
            val showNotifications = sharedPreferences.getBoolean(KEY_SHOW_NOTIFICATIONS, true)

            putNotificationLevel(if (showNotifications) NotificationLevel.MEDIUM else NotificationLevel.NONE)
        }
    }

    var notificationLevel by observable(
        sharedPreferences.getInt(KEY_NOTIFICATION_LEVEL, 1).let { NotificationLevel.values()[it] }
    ) { _, _, newValue -> putNotificationLevel(newValue) }

    private var languageInt by intObservable(KEY_LANGUAGE, Language.DEFAULT.ordinal)
    var language by observable(Language.values()[languageInt]) { _, _, newValue -> languageInt = newValue.ordinal }

    private fun putNotificationLevel(notificationLevel: NotificationLevel) {
        sharedPreferences.edit { putInt(KEY_NOTIFICATION_LEVEL, notificationLevel.ordinal) }
    }

    private val showDeletedProperty = NonNullRelayProperty(sharedPreferences.getBoolean(KEY_SHOW_DELETED, false))
    var showDeleted by showDeletedProperty
    val showDeletedObservable = showDeletedProperty.observable.distinctUntilChanged()

    private val showAssignedProperty = NonNullRelayProperty(sharedPreferences.getBoolean(KEY_SHOW_ASSIGNED_TO, true))
    var showAssigned by showAssignedProperty
    val showAssignedObservable = showAssignedProperty.observable.distinctUntilChanged()

    private var showProjectsProperty = NonNullRelayProperty(sharedPreferences.getBoolean(KEY_SHOW_PROJECTS, false))
    var showProjects by showProjectsProperty
    val showProjectsObservable = showProjectsProperty.observable.distinctUntilChanged()

    var instanceWarningSnooze: ExactTimeStamp.Local?
        get() = sharedPreferences.getLong(KEY_INSTANCE_WARNING_SNOOZE, -1)
            .takeIf { it != -1L }
            ?.let { ExactTimeStamp.Local(it) }
        set(value) {
            sharedPreferences.edit {
                putLong(KEY_INSTANCE_WARNING_SNOOZE, value?.long ?: -1)
            }
        }

    var instanceWarningSnoozeSet: Boolean
        get() = instanceWarningSnooze?.let {
            val oneDayAgo = DateTimeTz.nowLocal() - 1.days

            it.toDateTimeTz() > oneDayAgo
        } == true
        set(value) {
            instanceWarningSnooze = if (value) ExactTimeStamp.Local.now else null
        }

    init {
        showDeletedObservable.skip(1)
            .subscribe { sharedPreferences.edit { putBoolean(KEY_SHOW_DELETED, it) } }
            .ignore()

        showAssignedObservable.skip(1)
            .subscribe { sharedPreferences.edit { putBoolean(KEY_SHOW_ASSIGNED_TO, it) } }
            .ignore()

        showProjectsObservable.skip(1)
            .subscribe { sharedPreferences.edit { putBoolean(KEY_SHOW_PROJECTS, it) } }
            .ignore()
    }

    fun getTooltipShown(type: TooltipManager.Type) =
        sharedPreferences.getBoolean(KEY_TOOLTIP_SHOWN + type, false)

    fun setTooltipShown(type: TooltipManager.Type, shown: Boolean = true) =
        sharedPreferences.edit { putBoolean(KEY_TOOLTIP_SHOWN + type, shown) }

    private var projectOrderString: String by observable(
        sharedPreferences.getString(KEY_PROJECT_ORDER, "")!!
    ) { _, _, newValue -> sharedPreferences.edit { putString(KEY_PROJECT_ORDER, newValue) } }

    var projectOrder: Map<ProjectKey.Shared, Float> by observable(
        deserialize<HashMap<ProjectKey.Shared, Float>>(projectOrderString) ?: mapOf()
    ) { _, _, newValue -> projectOrderString = serialize(HashMap(newValue)) }
        private set

    private val projectFilterProperty =
        NonNullRelayProperty(sharedPreferences.getString(KEY_PROJECT_FILTER, null).let(ProjectFilter::fromJson))

    var projectFilter by projectFilterProperty
    val projectFilterObservable = projectFilterProperty.observable

    private const val PROJECT_ORDER_INCREMENT = 1 / 20f

    fun updateProjectOrder(addedProjectKey: ProjectKey.Shared, allProjectKeys: Set<ProjectKey.Shared>) {
        val oldProjectOrder = projectOrder.filterKeys { it in allProjectKeys }

        val oldSum = oldProjectOrder.values.sum()
        val newProjectOrder = if (oldSum > 0) {
            val oldTargetSum = 1 - PROJECT_ORDER_INCREMENT

            val weight = (oldTargetSum / oldSum).coerceAtMost(1f)
            oldProjectOrder.mapValues { it.value * weight }.toMutableMap()
        } else {
            oldProjectOrder.toMutableMap()
        }

        newProjectOrder[addedProjectKey] = newProjectOrder.getOrDefault(addedProjectKey, 0f) + PROJECT_ORDER_INCREMENT

        projectOrder = newProjectOrder
    }

    val notificationLog = Logger(KEY_NOTIFICATION_LOG, 100)

    private open class ReadOnlyStrPref(protected val key: String) : ReadOnlyProperty<Any, String> {

        final override fun getValue(
            thisRef: Any,
            property: KProperty<*>,
        ) = sharedPreferences.getString(key, "")!!
    }

    private class ReadWriteStrPref(key: String) : ReadOnlyStrPref(key), ReadWriteProperty<Any, String> {

        override fun setValue(thisRef: Any, property: KProperty<*>, value: String) =
            sharedPreferences.edit { putString(key, value) }
    }

    class Logger(key: String, private val length: Int = 100) {

        private var logString by ReadWriteStrPref(key)

        private lateinit var lineList: List<String>

        val log get() = logString

        private fun runOnIo(action: () -> Unit) {
            Single.fromCallable(action)
                .subscribeOn(Schedulers.io())
                .subscribe()
        }

        fun logLineDate(line: String) {
            runOnIo {
                logLine("")
                logLine(ExactTimeStamp.Local.now.date.toString())
                logLineHour(line)
            }
        }

        fun logLineHour(line: String, separator: Boolean = false) {
            runOnIo {
                if (separator)
                    logLine("")

                logLine(ExactTimeStamp.Local.now.hourMilli.toString() + " " + line)
            }
        }

        private fun logLine(line: String) {
            MyCrashlytics.log("Preferences.logLine: $line")

            if (!this::lineList.isInitialized) lineList = logString.split('\n')

            lineList = lineList.take(length)
                .toMutableList()
                .apply { add(0, line) }

            logString = lineList.joinToString("\n")
        }
    }

    enum class NotificationLevel {

        NONE, MEDIUM, HIGH
    }

    enum class Language {
        DEFAULT {

            override val locale get() = MyApplication.instance.defaultLocale

            override fun applySettingStartup() = LanguageSetting.setLanguage(MyApplication.context, locale)
        },
        ENGLISH {

            override val locale: Locale = Locale.ENGLISH
        },
        POLISH {

            override val locale = Locale("pl")
        };

        protected abstract val locale: Locale

        open fun applySettingStartup() {}

        fun applySetting(activity: AbstractActivity) {
            activity.setLanguage(locale)

            applySettingStartup()
        }
    }

    sealed class ProjectFilter {

        companion object {

            fun fromJson(json: String?) = when (json) {
                null, All.key -> All
                Private.key -> Private
                else -> Shared(ProjectKey.Shared(json))
            }
        }

        abstract fun toJson(): String

        object All : ProjectFilter() {

            val key = "all"

            override fun toJson() = key
        }

        object Private : ProjectFilter() {

            val key = "private"

            override fun toJson() = key
        }

        data class Shared(val projectKey: ProjectKey.Shared) : ProjectFilter() {

            override fun toJson() = projectKey.key
        }
    }
}