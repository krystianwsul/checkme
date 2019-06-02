package com.krystianwsul.checkme.firebase

interface RemoteMyUserInterface {

    fun setToken(token: String?)

    var defaultReminder: Boolean
}