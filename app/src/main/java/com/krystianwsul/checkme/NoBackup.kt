package com.krystianwsul.checkme

import android.content.Context
import androidx.core.content.edit
import java.util.*

object NoBackup {

    private const val NAME = "noBackup"
    private const val KEY_UUID = "uuid"

    private val sharedPreferences by lazy { MyApplication.context.getSharedPreferences(NAME, Context.MODE_PRIVATE) }

    val uuid: String

    init {
        if (sharedPreferences.contains(KEY_UUID)) {
            uuid = sharedPreferences.getString(KEY_UUID, "")!!
            check(uuid.isNotEmpty())
        } else {
            uuid = UUID.randomUUID().toString()

            sharedPreferences.edit { putString(KEY_UUID, uuid) }
        }
    }
}