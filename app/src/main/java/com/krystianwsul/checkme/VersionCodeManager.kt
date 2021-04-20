package com.krystianwsul.checkme

object VersionCodeManager {

    fun check(onUpgrade: () -> Unit) {
        if (Preferences.versionCode != BuildConfig.VERSION_CODE) {
            onUpgrade()
            Preferences.setVersionCode(BuildConfig.VERSION_CODE)
        }
    }
}