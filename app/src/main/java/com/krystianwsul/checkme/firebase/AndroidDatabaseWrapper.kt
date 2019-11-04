package com.krystianwsul.checkme.firebase

import com.androidhuman.rxfirebase2.database.childEvents
import com.androidhuman.rxfirebase2.database.data
import com.androidhuman.rxfirebase2.database.dataChanges
import com.google.firebase.database.FirebaseDatabase
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.utils.getMessage
import com.krystianwsul.common.firebase.DatabaseCallback
import com.krystianwsul.common.firebase.DatabaseWrapper


object AndroidDatabaseWrapper : DatabaseWrapper() {

    val root: String by lazy {
        MyApplication.instance
                .resources
                .getString(R.string.firebase_root)
    }

    private val userInfo get() = MyApplication.instance.userInfo

    private val rootReference by lazy {
        FirebaseDatabase.getInstance()
                .reference
                .child(root)
    }

    fun getUserDataDatabaseReference(key: String) = rootReference.child("$USERS_KEY/$key/userData")

    fun addFriend(friendKey: String) = rootReference.child("$USERS_KEY/$friendKey/friendOf/${userInfo.key}").setValue(true)

    fun addFriends(friendKeys: Set<String>) = rootReference.child(USERS_KEY).updateChildren(friendKeys.map { "$it/friendOf/${userInfo.key}" to true }.toMap())

    fun getFriendSingle(key: String) = rootReference.child(USERS_KEY)
            .orderByChild("friendOf/$key")
            .equalTo(true)
            .data()

    fun getFriendObservable(key: String) = rootReference.child(USERS_KEY)
            .orderByChild("friendOf/$key")
            .equalTo(true)
            .dataChanges()

    override fun getNewId(path: String) = rootReference.child(path)
            .push()
            .key!!

    private fun sharedProjectQuery(key: String) = rootReference.child(RECORDS_KEY)
            .orderByChild("recordOf/$key")
            .equalTo(true)

    fun getSharedProjectSingle(key: String) = sharedProjectQuery(key).data()

    fun getSharedProjectEvents(key: String) = sharedProjectQuery(key).childEvents()

    override fun update(path: String, values: Map<String, Any?>, callback: DatabaseCallback) {
        rootReference.child(path)
                .updateChildren(values)
                .addOnCompleteListener { callback(it.getMessage(), it.isSuccessful, it.exception) }
    }

    private fun privateProjectQuery(key: String) = rootReference.child("$PRIVATE_PROJECTS_KEY/$key")

    fun getPrivateProjectSingle(key: String) = privateProjectQuery(key).data()

    fun getPrivateProjectObservable(key: String) = privateProjectQuery(key).dataChanges()

    fun updateFriends(values: Map<String, Any?>) = rootReference.child(USERS_KEY).updateChildren(values)

    fun getUserSingle(key: String) = rootReference.child("$USERS_KEY/$key").data()

    fun getUserObservable(key: String) = rootReference.child("$USERS_KEY/$key").dataChanges()
}
