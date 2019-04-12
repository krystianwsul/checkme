package com.krystianwsul.checkme.firebase

sealed class ImageState {

    data class Local(val uuid: String) : ImageState()
    data class Remote(val uuid: String) : ImageState()
    object Uploading : ImageState()
}