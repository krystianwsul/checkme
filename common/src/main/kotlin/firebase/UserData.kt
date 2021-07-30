package com.krystianwsul.common.firebase

import com.krystianwsul.common.utils.Parcelable
import com.krystianwsul.common.utils.Parcelize
import com.krystianwsul.common.utils.UserKey
import com.krystianwsul.common.utils.toBase64

@Parcelize
data class UserData(
        val email: String = "",
        val name: String = "",
        val photoUrl: String? = null,
) : Parcelable {

    companion object {

        fun getKey(email: String) = UserKey(
                email.trim { it <= ' ' }
                        .toLowerCase()
                        .toBase64()
        )
    }
}
