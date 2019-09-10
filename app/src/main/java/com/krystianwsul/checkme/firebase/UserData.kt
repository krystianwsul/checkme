package com.krystianwsul.checkme.firebase

import android.os.Parcelable
import com.krystianwsul.common.utils.toBase64

import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
data class UserData(
        val email: String = "",
        val name: String = "",
        val photoUrl: String? = null) : Parcelable {

    companion object {

        fun getKey(email: String) = email.trim { it <= ' ' }
                .toLowerCase(Locale.ROOT)
                .toBase64()
    }

    fun getKey() = getKey(email)
}
