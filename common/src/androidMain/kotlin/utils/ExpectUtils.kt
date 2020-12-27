package com.krystianwsul.common.utils

import android.util.Base64
import android.util.Log

actual typealias Serializable = java.io.Serializable
actual typealias Parcelize = kotlinx.parcelize.Parcelize
actual typealias Parcelable = android.os.Parcelable

actual fun String.toBase64() = Base64.encodeToString(toByteArray(charset("UTF-8")), Base64.URL_SAFE or Base64.NO_WRAP)!!

actual fun log(message: String) {
    Log.e("asdf", message)
}