package com.krystianwsul.checkme.firebase

import com.androidhuman.rxfirebase2.database.data
import com.androidhuman.rxfirebase2.database.dataChanges
import com.google.firebase.database.FirebaseDatabase
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.utils.getMessage
import com.krystianwsul.common.firebase.DatabaseCallback
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.UserKey


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

    fun getUserDataDatabaseReference(key: UserKey) = rootReference.child("$USERS_KEY/${key.key}/userData")

    // todo same change as for projects
    fun addFriend(friendKey: UserKey) = rootReference.child("$USERS_KEY/${friendKey.key}/friendOf/${userInfo.key}").setValue(true)

    fun addFriends(friendKeys: Set<UserKey>) = rootReference.child(USERS_KEY).updateChildren(friendKeys.map { "${it.key}/friendOf/${userInfo.key}" to true }.toMap())

    fun getFriendSingle(key: UserKey) = rootReference.child(USERS_KEY)
            .orderByChild("friendOf/${key.key}")
            .equalTo(true)
            .data()

    fun getFriendObservable(key: UserKey) = rootReference.child(USERS_KEY)
            .orderByChild("friendOf/${key.key}")
            .equalTo(true)
            .dataChanges()

    override fun getNewId(path: String) = rootReference.child(path)
            .push()
            .key!!

    private fun sharedProjectQuery(projectKey: ProjectKey.Shared) = rootReference.child("$RECORDS_KEY/${projectKey.key}")

    fun getSharedProjectSingle(projectKey: ProjectKey.Shared) = sharedProjectQuery(projectKey).data()
    fun getSharedProjectObservable(projectKey: ProjectKey.Shared) = sharedProjectQuery(projectKey).dataChanges()

    override fun update(path: String, values: Map<String, Any?>, callback: DatabaseCallback) {
        rootReference.child(path)
                .updateChildren(values)
                .addOnCompleteListener { callback(it.getMessage(), it.isSuccessful, it.exception) }
    }

    private fun privateProjectQuery(key: ProjectKey.Private) = rootReference.child("$PRIVATE_PROJECTS_KEY/${key.key}")

    fun getPrivateProjectSingle(key: ProjectKey.Private) = privateProjectQuery(key).data()

    fun getPrivateProjectObservable(key: ProjectKey.Private) = privateProjectQuery(key).dataChanges()

    fun getUserSingle(key: UserKey) = rootReference.child("$USERS_KEY/${key.key}").data()

    fun getUserObservable(key: UserKey) = rootReference.child("$USERS_KEY/${key.key}").dataChanges()
}
