package com.krystianwsul.checkme

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.iid.FirebaseInstanceIdService
import com.krystianwsul.checkme.domainmodel.UserInfo
import com.krystianwsul.checkme.notifications.InstanceDoneService
import com.krystianwsul.checkme.persistencemodel.SaveService

class MyFirebaseInstanceIdService : FirebaseInstanceIdService() {

    companion object {

        val token get() = FirebaseInstanceId.getInstance().token
    }

    override fun onTokenRefresh() {
        Log.e("asdf", "onTokenRefresh $token")

        val firebaseUser = FirebaseAuth.getInstance().currentUser ?: return

        val userInfo = UserInfo(firebaseUser)

        InstanceDoneService.throttleFirebase(this, true, { it.updateUserInfo(this, SaveService.Source.SERVICE, userInfo) })
    }
}
