package com.krystianwsul.checkme.firebase

import com.androidhuman.rxfirebase2.database.dataChanges
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.GenericTypeIndicator
import com.google.firebase.database.Query
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.RemoteConfig
import com.krystianwsul.checkme.domainmodel.observeOnDomain
import com.krystianwsul.checkme.firebase.loaders.FactoryProvider
import com.krystianwsul.checkme.firebase.snapshot.IndicatorSnapshot
import com.krystianwsul.checkme.firebase.snapshot.TypedSnapshot
import com.krystianwsul.checkme.firebase.snapshot.UntypedSnapshot
import com.krystianwsul.checkme.utils.getMessage
import com.krystianwsul.checkme.utils.toV3
import com.krystianwsul.common.firebase.DatabaseCallback
import com.krystianwsul.common.firebase.json.InstanceJson
import com.krystianwsul.common.firebase.json.JsonWrapper
import com.krystianwsul.common.firebase.json.PrivateProjectJson
import com.krystianwsul.common.firebase.json.UserWrapper
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
    override fun getUserObservable(userKey: UserKey) = getUserQuery(userKey).typedSnapshotChanges<UserWrapper>()

    fun getUsersObservable() = rootReference.child(USERS_KEY)
            .orderByKey()
            .snapshotChanges()

    private fun Query.snapshotChanges() = dataChanges().toV3()
            .map<UntypedSnapshot>(UntypedSnapshot::Impl)
            .observeOnDomain()

    private fun <T : Any> Query.typedSnapshotChanges() = dataChanges().toV3()
            .map<TypedSnapshot<T>> { TypedSnapshot.Impl(it) }
            .observeOnDomain()

    private fun <T : Any> Query.indicatorSnapshotChanges() = dataChanges().toV3()
            .map<IndicatorSnapshot<T>> { IndicatorSnapshot.Impl(it) }
            .observeOnDomain()

    override fun getNewId(path: String) = rootReference.child(path)
            .push()
            .key!!

    private fun sharedProjectQuery(projectKey: ProjectKey.Shared) =
            rootReference.child("$RECORDS_KEY/${projectKey.key}")

    override fun getSharedProjectObservable(projectKey: ProjectKey.Shared) =
            sharedProjectQuery(projectKey).typedSnapshotChanges<JsonWrapper>()

    override fun update(values: Map<String, Any?>, callback: DatabaseCallback) {
        rootReference.updateChildren(values).addOnCompleteListener {
            callback(it.getMessage(), it.isSuccessful, it.exception)
        }
    }

    private fun privateProjectQuery(key: ProjectKey.Private) =
            rootReference.child("$PRIVATE_PROJECTS_KEY/${key.key}")

    override fun getPrivateProjectObservable(key: ProjectKey.Private) =
            privateProjectQuery(key).typedSnapshotChanges<PrivateProjectJson>()

    private fun rootInstanceQuery(taskFirebaseKey: String) =
            rootReference.child("$KEY_INSTANCES/$taskFirebaseKey")

    override fun getRootInstanceObservable(taskFirebaseKey: String): Observable<IndicatorSnapshot<Map<String, Map<String, InstanceJson>>>> {
        return RemoteConfig.observable
                .map { it.queryRemoteInstances }
                .distinctUntilChanged()
                .observeOnDomain()
                .switchMap {
                    if (it)
                        rootInstanceQuery(taskFirebaseKey).indicatorSnapshotChanges()
                    else
                        Observable.just(EmptyIndicatorSnapshot())
                }
    }

    private class EmptyIndicatorSnapshot<T : Any> : IndicatorSnapshot<T> {

        override val key get() = throw UnsupportedOperationException()

        override fun exists() = false

        override fun <T> getValue(genericTypeIndicator: GenericTypeIndicator<T>) = throw UnsupportedOperationException()
    }
}
