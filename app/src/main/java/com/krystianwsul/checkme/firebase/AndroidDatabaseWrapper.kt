package com.krystianwsul.checkme.firebase

import com.androidhuman.rxfirebase2.database.dataChanges
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.firebase.loaders.FactoryProvider
import com.krystianwsul.checkme.firebase.loaders.Snapshot
import com.krystianwsul.checkme.utils.getMessage
import com.krystianwsul.common.firebase.DatabaseCallback
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.UserKey


object AndroidDatabaseWrapper : FactoryProvider.Database() {

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

    private fun getUserQuery(userKey: UserKey) = rootReference.child("$USERS_KEY/${userKey.key}")

    fun getUserDataDatabaseReference(userKey: UserKey) = getUserQuery(userKey).child("userData")

    // todo same change as for projects
    fun addFriend(userKey: UserKey) = getUserQuery(userKey).child("friendOf/${userInfo.key.key}").setValue(true)

    fun addFriends(friendKeys: Set<UserKey>) = rootReference.child(USERS_KEY).updateChildren(
            friendKeys.map { "${it.key}/friendOf/${userInfo.key.key}" to true }.toMap()
    )

    private fun getFriendQuery(userKey: UserKey) = rootReference.child(USERS_KEY)
            .orderByChild("friendOf/${userKey.key}")
            .equalTo(true)

    private fun Query.snapshotChanges() = dataChanges().map<Snapshot> {
        Snapshot.Impl(it)
    }!!

    override fun getFriendObservable(userKey: UserKey) = getFriendQuery(userKey).snapshotChanges()

    override fun getNewId(path: String) = rootReference.child(path)
            .push()
            .key!!

    private fun sharedProjectQuery(projectKey: ProjectKey.Shared) = rootReference.child("$RECORDS_KEY/${projectKey.key}")
    override fun getSharedProjectObservable(projectKey: ProjectKey.Shared) = sharedProjectQuery(projectKey).snapshotChanges()

    override fun update(path: String, values: Map<String, Any?>, callback: DatabaseCallback) {
        rootReference.child(path)
                .updateChildren(values)
                .addOnCompleteListener { callback(it.getMessage(), it.isSuccessful, it.exception) }
    }

    private fun privateProjectQuery(key: ProjectKey.Private) = rootReference.child("$PRIVATE_PROJECTS_KEY/${key.key}")
    override fun getPrivateProjectObservable(key: ProjectKey.Private) = privateProjectQuery(key).snapshotChanges()

    fun updateFriends(values: Map<String, Any?>) = rootReference.child(USERS_KEY).updateChildren(values)

    override fun getUserObservable(key: UserKey) = rootReference.child("$USERS_KEY/${key.key}").snapshotChanges()

    private fun rootInstanceQuery(taskFirebaseKey: String) = rootReference.child("$KEY_INSTANCES/$taskFirebaseKey")
    override fun getRootInstanceObservable(taskFirebaseKey: String) = rootInstanceQuery(taskFirebaseKey).snapshotChanges()
}
