package com.krystianwsul.checkme

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.TickData
import com.krystianwsul.checkme.persistencemodel.SaveService


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
                DomainFactory.setFirebaseTickListener(SaveService.Source.SERVICE, TickData.Lock(false, "MyFirebaseMessagingService"))
        } else {
            MyCrashlytics.logException(UnknownMessageException(data))
        }
    }

    override fun onNewToken(token: String) {
        MyApplication.instance.token = token

        DomainFactory.addFirebaseListener { it.updateToken(SaveService.Source.SERVICE, token) }
    }

    private class UnknownMessageException(data: Map<String, String>) : Exception(getMessage(data)) {

        companion object {

            private fun getMessage(data: Map<String, String>) = data.entries.joinToString("\n") { it.key + ": " + it.value }
        }
    }
}
