package com.krystianwsul.common.firebase

data class ChangeWrapper<T>(val changeType: ChangeType, val data: T) {

    fun <U> newData(data: U) = ChangeWrapper(changeType, data)
}