package com.krystianwsul.common.firebase

enum class ChangeType {

    LOCAL, REMOTE;

    companion object {

        fun reduce(vararg changeTypes: ChangeType) = changeTypes.toList().reduce()
    }
}