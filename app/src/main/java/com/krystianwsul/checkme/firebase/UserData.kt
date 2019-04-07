package com.krystianwsul.checkme.firebase

import android.os.Parcelable
import android.text.TextUtils
import android.util.Base64
import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties

import kotlinx.android.parcel.Parcelize

@IgnoreExtraProperties
@Parcelize
data class UserData(
        val email: String = "",
        val name: String = "",
        val photoUrl: String? = null) : Parcelable {

    val key: String
        @Exclude
        get() = getKey(email)

    companion object {

        fun getKey(email: String): String {
            check(!TextUtils.isEmpty(email))

            val encoded = email.trim { it <= ' ' }
                    .toLowerCase()
                    .toByteArray(charset("UTF-8"))
            return Base64.encodeToString(encoded, Base64.URL_SAFE or Base64.NO_WRAP)
        }
    }
}
