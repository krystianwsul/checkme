package com.krystianwsul.common.firebase

enum class ChangeType {

    LOCAL, REMOTE;

    companion object {

        fun combine(a: ChangeType, b: ChangeType) = if (listOf(a, b).contains(REMOTE)) REMOTE else LOCAL
    }
}