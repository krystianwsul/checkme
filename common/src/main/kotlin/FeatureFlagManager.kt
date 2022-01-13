package com.krystianwsul.common

import com.krystianwsul.common.utils.flow.BehaviorFlow

object FeatureFlagManager {

    private val map = mutableMapOf<Flag, BehaviorFlow<Boolean>>()

    fun getFlag(flag: Flag) = map[flag]?.valueOrNull ?: false

    fun getFlags() = Flag.values().associateWith(::getFlag)

    fun getFlow(flag: Flag): BehaviorFlow<Boolean> {
        if (!map.containsKey(flag)) map[flag] = BehaviorFlow()

        return map.getValue(flag)
    }

    fun setFlag(flag: Flag, value: Boolean) {
        getFlow(flag).value = value
    }

    enum class Flag {

        LOG_NOT_DONE_PERFORMANCE
    }
}