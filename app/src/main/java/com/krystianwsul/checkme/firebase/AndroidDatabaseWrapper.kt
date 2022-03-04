package com.krystianwsul.checkme.firebase

import android.util.Log
import com.androidhuman.rxfirebase2.database.dataChanges
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.GenericTypeIndicator
import com.google.firebase.database.Query
import com.google.firebase.database.core.Path
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.domainmodel.HasInstancesStore
import com.krystianwsul.checkme.domainmodel.observeOnDomain
import com.krystianwsul.checkme.firebase.loaders.FactoryProvider
import com.krystianwsul.checkme.firebase.snapshot.Snapshot
import com.krystianwsul.checkme.utils.getMessage
import com.krystianwsul.checkme.utils.toSingleTask
import com.krystianwsul.checkme.utils.toV3
import com.krystianwsul.checkme.viewmodels.NullableWrapper
import com.krystianwsul.common.firebase.DatabaseCallback
import com.krystianwsul.common.firebase.json.JsonWrapper
import com.krystianwsul.common.firebase.json.Parsable
import com.krystianwsul.common.firebase.json.projects.PrivateProjectJson
import com.krystianwsul.common.firebase.json.tasks.RootTaskJson
import com.krystianwsul.common.firebase.json.users.UserWrapper
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.TaskKey
import com.krystianwsul.common.utils.UserKey
import com.mindorks.scheduler.Priority
import com.mindorks.scheduler.internal.CustomPriorityScheduler
import com.pacoworks.rxpaper2.RxPaperBook
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers


object AndroidDatabaseWrapper : FactoryProvider.Database() {

    private const val ENABLE_PAPER = true

    private const val FORCE_PRODUCTION = false

    val root: String by lazy {
        if (FORCE_PRODUCTION) {
            "production"
        } else {
            MyApplication.instance
                .resources
                .getString(R.string.firebase_root)
        }
    }

    private val rootReference by lazy {
        FirebaseDatabase.getInstance()
            .reference
            .child(root)
    }

    private val rxPaperBook = RxPaperBook.with("firebaseCache")

    fun onUpgrade() {
        rxPaperBook.destroy().subscribe()
    }

    private fun Path.toKey() = toString().replace('/', '-')

    private inline fun <reified T : Any> writeNullable(path: Path, value: T?): Completable {
        return if (ENABLE_PAPER) {
            rxPaperBook.write(path.toKey(), NullableWrapper(value))
                .toV3()
                .subscribeOn(Schedulers.io())
        } else {
            Completable.complete()
        }
    }

    private inline fun <reified T : Any> readNullable(path: Path): Maybe<NullableWrapper<T>> {
        return if (ENABLE_PAPER) {
            rxPaperBook.read<NullableWrapper<T>>(path.toKey())
                .toV3()
                .subscribeOn(Schedulers.io())
                .toMaybe()
                .onErrorComplete()
        } else {
            Maybe.empty()
        }
    }

    private class SnapshotConverter<T : Parsable>(path: Path) : Converter<NullableWrapper<T>, Snapshot<T>>(
        { Snapshot(path.back.asString(), it.value) },
        { NullableWrapper(it.value) },
    )

    private inline fun <reified T : Parsable> Query.typedSnapshotChanges(read: Read<T>): Observable<Snapshot<T>> = cache(
        read,
        { Snapshot.fromParsable(it, T::class) },
        SnapshotConverter(path),
        { readNullable(it) },
        { path, value -> writeNullable(path, value) },
    )

    private inline fun <reified T : Any> Query.indicatorSnapshotChanges(read: Read<T>): Observable<Snapshot<T>> = cache(
        read,
        { Snapshot.fromTypeIndicator(it, object : GenericTypeIndicator<T>() {}) },
        Converter(
            { Snapshot(path.back.asString(), it.value) },
            { NullableWrapper(it.value) },
        ),
        { readNullable(it) },
        { path, value -> writeNullable(path, value) },
    )

    private fun <T : Any, U : Snapshot<T>> Query.cache(
        read: Read<T>,
        firebaseToSnapshot: (dataSnapshot: DataSnapshot) -> U,
        converter: Converter<NullableWrapper<T>, U>,
        readNullable: (path: Path) -> Maybe<NullableWrapper<T>>,
        writeNullable: (path: Path, T?) -> Completable,
    ): Observable<U> {

        val firebaseObservable = dataChanges().toV3()
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.computation())
            .map { firebaseToSnapshot(it) }
            .doOnNext { writeNullable(path, it.value).subscribe() }

        return mergePaperAndRx(readNullable(path), firebaseObservable, converter).flatMapSingle {
            Single.just(it).observeOnDomain(read.priority)
        }
            .doOnNext {
                Log.e("asdf", "magic db ${read.type} " + CustomPriorityScheduler.currentPriority.get()) // todo scheduling
            }
    }

    override fun getNewId(path: String) = rootReference.child(path)
        .push()
        .key!!

    override fun update(values: Map<String, Any?>, callback: DatabaseCallback) {
        rootReference.updateChildren(values)
            .toSingleTask()
            .observeOnDomain(Priority.IMMEDIATE)
            .subscribe { task -> callback(task.getMessage(), task.isSuccessful, task.exception) }
    }

    private fun getUserQuery(userKey: UserKey) = rootReference.child("$USERS_KEY/${userKey.key}")

    override fun getUserObservable(userKey: UserKey) = UserRead(userKey).getResult()

    private fun privateProjectQuery(key: ProjectKey.Private) =
        rootReference.child("$PRIVATE_PROJECTS_KEY/${key.key}")

    override fun getPrivateProjectObservable(projectKey: ProjectKey.Private) = PrivateProjectRead(projectKey).getResult()

    private fun sharedProjectQuery(projectKey: ProjectKey.Shared) =
        rootReference.child("$RECORDS_KEY/${projectKey.key}")

    override fun getSharedProjectObservable(projectKey: ProjectKey.Shared) = SharedProjectRead(projectKey).getResult()

    private fun rootTaskQuery(rootTaskKey: TaskKey.Root) =
        rootReference.child("$TASKS_KEY/${rootTaskKey.taskId}")

    override fun getRootTaskObservable(taskKey: TaskKey.Root) = TaskRead(taskKey).getResult()

    fun getUsersObservable() = UsersRead().getResult()

    sealed class LoadState<T : Any> {

        class Initial<T : Any> : LoadState<T>()

        class Loaded<T : Any>(val value: T) : LoadState<T>()
    }

    private interface Read<DATA : Any> {

        val type: String

        val priority get() = Priority.DB

        fun getResult(): Observable<Snapshot<DATA>>
    }

    private interface TypedRead<DATA : Parsable> : Read<DATA>

    private interface IndicatorRead<DATA : Any> : Read<DATA>

    private class UserRead(private val userKey: UserKey) : TypedRead<UserWrapper> {

        override val type = "user"

        override fun getResult() = getUserQuery(userKey).typedSnapshotChanges(this)
    }

    private class PrivateProjectRead(private val projectKey: ProjectKey.Private) : TypedRead<PrivateProjectJson> {

        override val type = "privateProject"

        override fun getResult() = privateProjectQuery(projectKey).typedSnapshotChanges(this)
    }

    private class SharedProjectRead(private val projectKey: ProjectKey.Shared) : TypedRead<JsonWrapper> {

        override val type = "sharedProject"

        override fun getResult() = sharedProjectQuery(projectKey).typedSnapshotChanges(this)
    }

    private class TaskRead(private val taskKey: TaskKey.Root) : TypedRead<RootTaskJson> {

        override val type = "task"

        override val priority get() = HasInstancesStore.getPriority(taskKey)

        override fun getResult() = rootTaskQuery(taskKey).typedSnapshotChanges(this)
    }

    private class UsersRead : IndicatorRead<Map<String, UserWrapper>> {

        override val type = "users"

        override fun getResult() = rootReference.child(USERS_KEY)
            .orderByKey()
            .indicatorSnapshotChanges(this)
    }
}
