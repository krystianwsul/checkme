package com.krystianwsul.checkme.firebase

data class ImageData(val uuid: String, val uploading: Boolean) {

    val image by lazy { if (uploading) uuid + RemoteTask.TMP_SUFFIX else uuid }
}