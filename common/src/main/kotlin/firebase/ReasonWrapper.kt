package com.krystianwsul.common.firebase

data class ReasonWrapper<T>(val userLoadReason: UserLoadReason, val value: T) {

    fun <U> newValue(value: U) = ReasonWrapper(userLoadReason, value)
}