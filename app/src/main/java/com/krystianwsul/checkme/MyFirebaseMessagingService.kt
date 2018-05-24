package com.krystianwsul.checkme

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.TickData
import com.krystianwsul.checkme.domainmodel.UserInfo
import com.krystianwsul.checkme.persistencemodel.SaveService
import junit.framework.Assert

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {

        private const val REFRESH_KEY = "refresh"
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.e("asdf", "remoteMessage.getData: " + remoteMessage.data)

        val data = remoteMessage.data!!

        if (data.containsKey(REFRESH_KEY)) {
            val refresh = data[REFRESH_KEY]!!
            Assert.assertTrue(refresh.isNotEmpty())
            Assert.assertTrue(data[REFRESH_KEY] == "true")

            val firebaseUser = FirebaseAuth.getInstance().currentUser
            if (firebaseUser != null) {
                val userInfo = UserInfo(firebaseUser)

                DomainFactory.getDomainFactory().let {
                    it.setUserInfo(this, SaveService.Source.SERVICE, userInfo)

                    it.setFirebaseTickListener(this, SaveService.Source.SERVICE, TickData(false, "MyFirebaseMessagingService", this, listOf()))
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
