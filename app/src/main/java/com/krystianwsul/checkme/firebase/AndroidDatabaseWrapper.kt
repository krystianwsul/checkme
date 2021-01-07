package com.krystianwsul.checkme.firebase

import com.androidhuman.rxfirebase2.database.dataChanges
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.GenericTypeIndicator
import com.google.firebase.database.Query
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.RemoteConfig
import com.krystianwsul.checkme.firebase.loaders.FactoryProvider
import com.krystianwsul.checkme.firebase.loaders.Snapshot
import com.krystianwsul.checkme.utils.getMessage
import com.krystianwsul.checkme.utils.toV3
import com.krystianwsul.common.firebase.DatabaseCallback
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.UserKey
import io.reactivex.rxjava3.core.Observable


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
    fun getUserDataDatabaseReference(userKey: UserKey) = getUserQuery(userKey)
    override fun getUserObservable(userKey: UserKey) = getUserQuery(userKey).snapshotChanges()

    private fun Query.snapshotChanges() = dataChanges().toV3().map<Snapshot>(Snapshot::Impl)!!

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

    override fun getRootInstanceObservable(taskFirebaseKey: String): Observable<Snapshot> {
        return RemoteConfig.observable
                .map { it.queryRemoteInstances }
                .distinctUntilChanged()
                .switchMap {
                    if (it)
                        rootInstanceQuery(taskFirebaseKey).snapshotChanges()
                    else
                        Observable.just(EmptySnapshot())
                }
    }

    private class EmptySnapshot : Snapshot {

        override val key get() = throw UnsupportedOperationException()

        override val children get() = throw UnsupportedOperationException()

        override fun exists() = false

        override fun <T> getValue(valueType: Class<T>) = throw UnsupportedOperationException()

        override fun <T> getValue(genericTypeIndicator: GenericTypeIndicator<T>): T? = null
    }
}
