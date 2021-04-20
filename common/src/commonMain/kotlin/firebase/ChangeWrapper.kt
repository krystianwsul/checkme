package com.krystianwsul.common.firebase

data class ChangeWrapper<T : Any>(val changeType: ChangeType, val data: T) {

    fun <U : Any> newData(data: U) = ChangeWrapper(changeType, data)
}