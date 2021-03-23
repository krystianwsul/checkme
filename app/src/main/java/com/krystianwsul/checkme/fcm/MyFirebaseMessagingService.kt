package com.krystianwsul.checkme.fcm

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.ticks.Ticker


class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {

        private const val REFRESH_KEY = "refresh"
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val data = remoteMessage.data

        if (data.containsKey(REFRESH_KEY)) {
            val refresh = data[REFRESH_KEY]!!
            check(refresh.isNotEmpty())
            check(data[REFRESH_KEY] == "true")

            if (MyApplication.instance.hasUserInfo)
                Ticker.tick("MyFirebaseMessagingService", true).subscribe()
        } else {
            MyCrashlytics.logException(UnknownMessageException(data))
        }
    }

    override fun onNewToken(token: String) {
        Preferences.token = token
    }

    private class UnknownMessageException(data: Map<String, String>) : Exception(getMessage(data)) {

        companion object {

            private fun getMessage(data: Map<String, String>) = data.entries.joinToString("\n") { it.key + ": " + it.value }
        }
    }
}
