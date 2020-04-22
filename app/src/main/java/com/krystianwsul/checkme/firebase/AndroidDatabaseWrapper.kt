package com.krystianwsul.checkme.firebase

import com.androidhuman.rxfirebase2.database.data
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

    private val rootReference by lazy {
        FirebaseDatabase.getInstance()
                .reference
                .child(root)
    }

    private fun getUserQuery(userKey: UserKey) = rootReference.child("$USERS_KEY/${userKey.key}")

    fun getUserDataDatabaseReference(userKey: UserKey) = getUserQuery(userKey).child("userData")
    fun getUserSingle(userKey: UserKey) = getUserQuery(userKey).snapshot()
    override fun getUserObservable(userKey: UserKey) = getUserQuery(userKey).snapshotChanges()

    private fun Query.snapshot() = data().map<Snapshot>(Snapshot::Impl)
    private fun Query.snapshotChanges() = dataChanges().map<Snapshot>(Snapshot::Impl)!!

    override fun getNewId(path: String) = rootReference.child(path)
            .push()
            .key!!

    private fun sharedProjectQuery(projectKey: ProjectKey.Shared) = rootReference.child("$RECORDS_KEY/${projectKey.key}")
    override fun getSharedProjectObservable(projectKey: ProjectKey.Shared) = sharedProjectQuery(projectKey).snapshotChanges()

    override fun update(values: Map<String, Any?>, callback: DatabaseCallback) {
        rootReference.updateChildren(values).addOnCompleteListener {
            callback(it.getMessage(), it.isSuccessful, it.exception)
        }
    }

    private fun privateProjectQuery(key: ProjectKey.Private) = rootReference.child("$PRIVATE_PROJECTS_KEY/${key.key}")
    override fun getPrivateProjectObservable(key: ProjectKey.Private) = privateProjectQuery(key).snapshotChanges()

    private fun rootInstanceQuery(taskFirebaseKey: String) = rootReference.child("$KEY_INSTANCES/$taskFirebaseKey")
    override fun getRootInstanceObservable(taskFirebaseKey: String) = rootInstanceQuery(taskFirebaseKey).snapshotChanges()
}
