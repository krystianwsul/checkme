package com.krystianwsul.common

object FeatureFlagManager {

    private val map = mutableMapOf<Flag, Boolean>()

    fun getFlag(flag: Flag) = map.getOrDefault(flag, false)

    fun getFlags() = Flag.values().associateWith(::getFlag)

    fun setFlag(flag: Flag, value: Boolean) {
        map[flag] = value
    }

    enum class Flag {

        NEW_SCHEDULE
    }
}