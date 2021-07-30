package com.krystianwsul.common.utils

@Parcelize
data class UserKey(val key: String) : Parcelable, Comparable<UserKey> {

    fun toPrivateProjectKey() = ProjectKey.Private(key)

    override fun compareTo(other: UserKey) = key.compareTo(other.key)
}