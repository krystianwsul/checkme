package com.krystianwsul.checkme

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.TickData
import com.krystianwsul.checkme.domainmodel.UserInfo
import com.krystianwsul.checkme.notifications.InstanceDoneService
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

            if (MyApplication.instance.hasUserInfo)
                DomainFactory.instance.setFirebaseTickListener(SaveService.Source.SERVICE, TickData(false, "MyFirebaseMessagingService", listOf()))
        } else {
            MyCrashlytics.logException(UnknownMessageException(data))
        }
    }

    override fun onNewToken(token: String) {
        Log.e("asdf", "onTokenRefresh $token")

        MyApplication.instance.token = token

        val firebaseUser = FirebaseAuth.getInstance().currentUser ?: return

        val userInfo = UserInfo(firebaseUser)

        InstanceDoneService.throttleFirebase(true) { it.updateUserInfo(SaveService.Source.SERVICE, userInfo) }
    }

    private class UnknownMessageException(data: Map<String, String>) : Exception(getMessage(data)) {

        companion object {

            private fun getMessage(data: Map<String, String>) = data.entries.joinToString("\n") { it.key + ": " + it.value }

        }
    }
}
