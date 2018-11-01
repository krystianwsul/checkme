package com.krystianwsul.checkme

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.krystianwsul.checkme.domainmodel.KotlinDomainFactory
import com.krystianwsul.checkme.domainmodel.TickData
import com.krystianwsul.checkme.domainmodel.UserInfo
import com.krystianwsul.checkme.persistencemodel.SaveService


class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {

        private const val REFRESH_KEY = "refresh"
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.e("asdf", "remoteMessage.getData: " + remoteMessage.data)

        val data = remoteMessage.data!!

        if (data.containsKey(REFRESH_KEY)) {
            val refresh = data[REFRESH_KEY]!!
            check(refresh.isNotEmpty())
            check(data[REFRESH_KEY] == "true")

            val firebaseUser = FirebaseAuth.getInstance().currentUser
            if (firebaseUser != null) {
                val userInfo = UserInfo(firebaseUser)

                KotlinDomainFactory.getKotlinDomainFactory().let {
                    it.setUserInfo(SaveService.Source.SERVICE, userInfo)

                    it.setFirebaseTickListener(SaveService.Source.SERVICE, TickData(false, "MyFirebaseMessagingService", listOf()))
                }
            }
        } else {
            MyCrashlytics.logException(UnknownMessageException(data))
        }
    }

    private class UnknownMessageException(data: Map<String, String>) : Exception(getMessage(data)) {

        companion object {

            private fun getMessage(data: Map<String, String>) = data.entries.joinToString("\n") { it.key + ": " + it.value }

        }
    }
}
