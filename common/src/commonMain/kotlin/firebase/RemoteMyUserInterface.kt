package com.krystianwsul.common.firebase

interface RemoteMyUserInterface {

    fun setToken(token: String?)

    var defaultReminder: Boolean
    var defaultTab: Int
}