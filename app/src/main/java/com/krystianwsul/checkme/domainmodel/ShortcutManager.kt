package com.krystianwsul.checkme.domainmodel

import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.utils.TaskKey
import org.joda.time.LocalDateTime

object ShortcutManager {

    fun addShortcut(taskKey: TaskKey) {
        val shortcuts = Preferences.shortcuts.toMutableMap()

        shortcuts[taskKey] = LocalDateTime.now()

        Preferences.shortcuts = shortcuts
    }

    fun keepShortcuts(taskKeys: List<TaskKey>) {
        val shortcuts = Preferences.shortcuts.toMutableMap().filterKeys { it in taskKeys }

        Preferences.shortcuts = shortcuts
    }

    fun getShortcuts() = Preferences.shortcuts
}