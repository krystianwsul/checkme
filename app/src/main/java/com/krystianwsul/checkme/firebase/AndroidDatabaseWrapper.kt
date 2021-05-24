package com.krystianwsul.checkme.firebase

import com.androidhuman.rxfirebase2.database.dataChanges
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.GenericTypeIndicator
import com.google.firebase.database.Query
import com.google.firebase.database.core.Path
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.domainmodel.observeOnDomain
import com.krystianwsul.checkme.firebase.loaders.FactoryProvider
import com.krystianwsul.checkme.firebase.snapshot.Snapshot
import com.krystianwsul.checkme.utils.getMessage
import com.krystianwsul.checkme.utils.toV3
import com.krystianwsul.checkme.viewmodels.NullableWrapper
import com.krystianwsul.common.firebase.DatabaseCallback
import com.krystianwsul.common.firebase.json.JsonWrapper
import com.krystianwsul.common.firebase.json.Parsable
import com.krystianwsul.common.firebase.json.UserWrapper
import com.krystianwsul.common.firebase.json.projects.PrivateProjectJson
import com.krystianwsul.common.firebase.json.tasks.RootTaskJson
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.TaskKey
import com.krystianwsul.common.utils.UserKey
import com.pacoworks.rxpaper2.RxPaperBook
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers


object AndroidDatabaseWrapper : FactoryProvider.Database() {

    private const val ENABLE_PAPER = true

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

    private val rxPaperBook = RxPaperBook.with("firebaseCache")

    fun onUpgrade() {
        rxPaperBook.destroy().subscribe()
    }

    private fun getUserQuery(userKey: UserKey) = rootReference.child("$USERS_KEY/${userKey.key}")
    override fun getUserObservable(userKey: UserKey) = getUserQuery(userKey).typedSnapshotChanges<UserWrapper>(false)

    fun getUsersObservable() = rootReference.child(USERS_KEY)
        .orderByKey()
        .indicatorSnapshotChanges<Map<String, UserWrapper>>()

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

    private class SnapshotConverter<T : Parsable>(
        path: Path,
        logDiff: Boolean,
        private val customPrintDiff: ((paper: T?, firebase: T?) -> String)? = null,
    ) : Converter<NullableWrapper<T>, Snapshot<T>>(
        { Snapshot(path.back.asString(), it.value) },
        { NullableWrapper(it.value) },
        logDiff,
    ) {

        override fun printDiff(paper: NullableWrapper<T>, firebase: NullableWrapper<T>) =
            customPrintDiff?.invoke(paper.value, firebase.value) ?: super.printDiff(paper, firebase)
    }

    private inline fun <reified T : Parsable> Query.typedSnapshotChanges(
        logDiff: Boolean,
        noinline customPrintDiff: ((paper: T?, firebase: T?) -> String)? = null,
    ): Observable<Snapshot<T>> {
        return cache(
            { Snapshot.fromParsable(it, T::class) },
            SnapshotConverter(path, logDiff, customPrintDiff),
            { readNullable(it) },
            { path, value -> writeNullable(path, value) },
        )
    }

    private inline fun <reified T : Any> Query.indicatorSnapshotChanges(): Observable<Snapshot<T>> =
        cache(
            { Snapshot.fromTypeIndicator(it, object : GenericTypeIndicator<T>() {}) },
            Converter(
                { Snapshot(path.back.asString(), it.value) },
                { NullableWrapper(it.value) },
                true,
            ),
            { readNullable(it) },
            { path, value -> writeNullable(path, value) },
        )

    private fun <T : Any, U : Snapshot<T>> Query.cache(
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

        return mergePaperAndRx(readNullable(path), firebaseObservable, converter).observeOnDomain()
    }

    override fun getNewId(path: String) = rootReference.child(path)
        .push()
        .key!!

    private fun sharedProjectQuery(projectKey: ProjectKey.Shared) =
        rootReference.child("$RECORDS_KEY/${projectKey.key}")

    override fun getSharedProjectObservable(projectKey: ProjectKey.Shared) =
        sharedProjectQuery(projectKey).typedSnapshotChanges<JsonWrapper>(true)

    override fun update(values: Map<String, Any?>, callback: DatabaseCallback) {
        rootReference.updateChildren(values).addOnCompleteListener {
            callback(it.getMessage(), it.isSuccessful, it.exception)
        }
    }

    private fun privateProjectQuery(key: ProjectKey.Private) =
        rootReference.child("$PRIVATE_PROJECTS_KEY/${key.key}")

    override fun getPrivateProjectObservable(key: ProjectKey.Private) =
        privateProjectQuery(key).typedSnapshotChanges<PrivateProjectJson>(true) { paper, firebase ->
            when {
                paper == null && firebase == null -> "both are null, WTF?"
                paper == null || firebase == null -> "one is null, paper: $paper, firebase: $firebase"
                else -> {
                    val stringBuilder = StringBuilder("fields are different: \n")

                    val fieldsList = listOf(
                        PrivateProjectJson::name,
                        PrivateProjectJson::startTime,
                        PrivateProjectJson::startTimeOffset,
                        PrivateProjectJson::endTime,
                        PrivateProjectJson::endTimeOffset,
                        PrivateProjectJson::tasks,
                        PrivateProjectJson::taskHierarchies,
                        PrivateProjectJson::customTimes,
                        PrivateProjectJson::defaultTimesCreated,
                        PrivateProjectJson::rootTaskIds,
                    )

                    fieldsList.forEach { field ->
                        val paperField = field.get(paper)
                        val firebaseField = field.get(firebase)

                        if (paperField != firebaseField) {
                            stringBuilder.appendLine("field ${field.name} different, paper: $paperField, firebase: $firebaseField ; ")
                        } else {
                            stringBuilder.appendLine("field ${field.name} same ; ")
                        }
                    }

                    stringBuilder.toString()
                }
            }

        }

    private fun rootTaskQuery(rootTaskKey: TaskKey.Root) =
        rootReference.child("$TASKS_KEY/${rootTaskKey.taskId}")

    override fun getRootTaskObservable(rootTaskKey: TaskKey.Root) =
        rootTaskQuery(rootTaskKey).typedSnapshotChanges<RootTaskJson>(true)

    sealed class LoadState<T : Any> {

        class Initial<T : Any> : LoadState<T>()

        class Loaded<T : Any>(val value: T) : LoadState<T>()
    }
}
