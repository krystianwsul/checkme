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
import com.krystianwsul.common.utils.ProjectKey


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

    // todo add UserKey class
    fun getUserDataDatabaseReference(key: ProjectKey.Private) = rootReference.child("$USERS_KEY/${key.key}/userData")

    // todo same change as for projects
    fun addFriend(friendKey: ProjectKey.Private) = rootReference.child("$USERS_KEY/${friendKey.key}/friendOf/${userInfo.key}").setValue(true)

    fun addFriends(friendKeys: Set<ProjectKey.Private>) = rootReference.child(USERS_KEY).updateChildren(friendKeys.map { "${it.key}/friendOf/${userInfo.key}" to true }.toMap())

    fun getFriendSingle(key: ProjectKey.Private) = rootReference.child(USERS_KEY)
            .orderByChild("friendOf/${key.key}")
            .equalTo(true)
            .data()

    fun getFriendObservable(key: ProjectKey.Private) = rootReference.child(USERS_KEY)
            .orderByChild("friendOf/${key.key}")
            .equalTo(true)
            .dataChanges()

    override fun getNewId(path: String) = rootReference.child(path)
            .push()
            .key!!

    private fun sharedProjectQuery(projectKey: ProjectKey) = rootReference.child(RECORDS_KEY)
            .orderByChild("recordOf/${projectKey.key}")
            .equalTo(true)

    /* todo
    Using an unspecified index. Your data will be downloaded and filtered on the client. Consider adding '".indexOn": "recordOf/cGF0cmljaXVzQGdtYWlsLmNvbQ=="' at production/records to your security and Firebase Database rules for better performance
     */

    fun getSharedProjectSingle(projectKey: ProjectKey) = sharedProjectQuery(projectKey).data()

    fun getSharedProjectEvents(projectKey: ProjectKey) = sharedProjectQuery(projectKey).childEvents()

    override fun update(path: String, values: Map<String, Any?>, callback: DatabaseCallback) {
        rootReference.child(path)
                .updateChildren(values)
                .addOnCompleteListener { callback(it.getMessage(), it.isSuccessful, it.exception) }
    }

    private fun privateProjectQuery(key: ProjectKey.Private) = rootReference.child("$PRIVATE_PROJECTS_KEY/${key.key}")

    fun getPrivateProjectSingle(key: ProjectKey.Private) = privateProjectQuery(key).data()

    fun getPrivateProjectObservable(key: ProjectKey.Private) = privateProjectQuery(key).dataChanges()

    fun updateFriends(values: Map<String, Any?>) = rootReference.child(USERS_KEY).updateChildren(values)

    fun getUserSingle(key: ProjectKey.Private) = rootReference.child("$USERS_KEY/${key.key}").data()

    fun getUserObservable(key: ProjectKey.Private) = rootReference.child("$USERS_KEY/${key.key}").dataChanges()
}
