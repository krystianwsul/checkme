package com.krystianwsul.common.firebase

import com.krystianwsul.common.utils.Parcelable
import com.krystianwsul.common.utils.Parcelize
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.toBase64

@Parcelize
data class UserData(
        val email: String = "",
        val name: String = "",
        val photoUrl: String? = null) : Parcelable {

    companion object {

        fun getKey(email: String) = ProjectKey.Private(email.trim { it <= ' ' }
                .toLowerCase()
                .toBase64())
    }

    fun getKey() = getKey(email)
}
