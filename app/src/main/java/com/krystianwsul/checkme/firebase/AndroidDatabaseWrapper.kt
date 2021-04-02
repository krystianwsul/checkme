package com.krystianwsul.checkme.firebase

import com.androidhuman.rxfirebase2.database.dataChanges
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.GenericTypeIndicator
import com.google.firebase.database.Query
import com.google.firebase.database.core.Path
import com.krystianwsul.checkme.RemoteConfig
import com.krystianwsul.checkme.domainmodel.observeOnDomain
import com.krystianwsul.checkme.firebase.loaders.FactoryProvider
import com.krystianwsul.checkme.firebase.snapshot.IndicatorSnapshot
import com.krystianwsul.checkme.firebase.snapshot.TypedSnapshot
import com.krystianwsul.checkme.firebase.snapshot.UntypedSnapshot
import com.krystianwsul.checkme.firebase.snapshot.ValueSnapshot
import com.krystianwsul.checkme.utils.getMessage
import com.krystianwsul.checkme.utils.toV3
import com.krystianwsul.checkme.viewmodels.NullableWrapper
import com.krystianwsul.common.firebase.DatabaseCallback
import com.krystianwsul.common.firebase.json.InstanceJson
import com.krystianwsul.common.firebase.json.JsonWrapper
import com.krystianwsul.common.firebase.json.PrivateProjectJson
import com.krystianwsul.common.firebase.json.UserWrapper
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.UserKey
import com.pacoworks.rxpaper2.RxPaperBook
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers


object AndroidDatabaseWrapper : FactoryProvider.Database() {

    val root: String = "production" // todo paper

    private val rootReference by lazy {
        FirebaseDatabase.getInstance()
                .reference
                .child(root)
    }

    private val rxPaperBook = RxPaperBook.with("firebaseCache")

    private fun getUserQuery(userKey: UserKey) = rootReference.child("$USERS_KEY/${userKey.key}")
    override fun getUserObservable(userKey: UserKey) = getUserQuery(userKey).typedSnapshotChanges<UserWrapper>()

    fun getUsersObservable() = rootReference.child(USERS_KEY)
            .orderByKey()
            .snapshotChanges()

    private fun Query.snapshotChanges() = dataChanges().toV3()
            .map<UntypedSnapshot>(UntypedSnapshot::Impl)
            .observeOnDomain()

    private fun Path.toKey() = toString().replace('/', '-')

    private inline fun <reified T : Any> writeNullable(path: Path, value: T?) =
            rxPaperBook.write(path.toKey(), NullableWrapper(value)).toV3()

    private inline fun <reified T : Any> readNullable(path: Path): Maybe<NullableWrapper<T>> {
        val key = path.toKey()

        return rxPaperBook.contains(key)
                .toV3()
                .filter { it }
                .flatMapSingle { rxPaperBook.read<NullableWrapper<T>>(path.toKey()).toV3() }
    }

    private inline fun <reified T : Any> Query.typedSnapshotChanges(): Observable<TypedSnapshot<T>> =
            cache(
                    { TypedSnapshot.Impl(it, T::class) },
                    { TypedSnapshot.Wrapper(path.back.asString(), it.value) },
                    { readNullable(it) },
                    { path, value -> writeNullable(path, value) },
            )

    private inline fun <reified T : Any> Query.indicatorSnapshotChanges(): Observable<IndicatorSnapshot<T>> =
            cache(
                    { IndicatorSnapshot.Impl(it, object : GenericTypeIndicator<T>() {}) },
                    { IndicatorSnapshot.Wrapper(path.back.asString(), it.value) },
                    { readNullable(it) },
                    { path, value -> writeNullable(path, value) },
            )

    private fun <T : Any, U : ValueSnapshot<T>> Query.cache(
            firebaseToSnapshot: (dataSnapshot: DataSnapshot) -> U,
            paperToSnapshot: (NullableWrapper<T>) -> U,
            readNullable: (path: Path) -> Maybe<NullableWrapper<T>>,
            writeNullable: (path: Path, T?) -> Completable,
    ): Observable<U> {
        val firebaseObservable = dataChanges()
                .toV3()
                .observeOn(Schedulers.computation())
                .map { firebaseToSnapshot(it) }
                .share()

        firebaseObservable.flatMapCompletable { writeNullable(path, it.getValue()) }.subscribe()

        return mergePaperAndRx(readNullable(path), firebaseObservable, paperToSnapshot).observeOnDomain()
    }

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

        override fun getValue(): T? = null
    }

    sealed class LoadState<T : Any> {

        class Initial<T : Any> : LoadState<T>()

        class Loaded<T : Any>(val value: T) : LoadState<T>()
    }
}
