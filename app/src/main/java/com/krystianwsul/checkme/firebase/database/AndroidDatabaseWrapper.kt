package com.krystianwsul.checkme.firebase

import com.google.firebase.database.FirebaseDatabase
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.domainmodel.observeOnDomain
import com.krystianwsul.checkme.firebase.database.*
import com.krystianwsul.checkme.firebase.loaders.FactoryProvider
import com.krystianwsul.checkme.utils.getMessage
import com.krystianwsul.checkme.utils.toSingleTask
import com.krystianwsul.common.firebase.DatabaseCallback
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.TaskKey
import com.krystianwsul.common.utils.UserKey
import com.mindorks.scheduler.Priority
import com.pacoworks.rxpaper2.RxPaperBook
import io.reactivex.rxjava3.core.Observable
import java.util.*


object AndroidDatabaseWrapper : FactoryProvider.Database() {

    const val ENABLE_PAPER = true

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

    val rootReference by lazy {
        FirebaseDatabase.getInstance()
            .reference
            .child(root)
    }

    val rxPaperBook = RxPaperBook.with("firebaseCache")

    fun onUpgrade() {
        rxPaperBook.destroy().subscribe()
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

    private val userTracker = Tracker<UserKey>()
    override fun getUserObservable(userKey: UserKey) = UserDatabaseRead(userKey).getResult().track(userTracker, userKey)

    private val privateProjectTracker = Tracker<ProjectKey.Private>()
    override fun getPrivateProjectObservable(projectKey: ProjectKey.Private) =
        PrivateProjectDatabaseRead(projectKey).getResult().track(privateProjectTracker, projectKey)

    private val sharedProjectTracker = Tracker<ProjectKey.Shared>()
    override fun getSharedProjectObservable(projectKey: ProjectKey.Shared) =
        SharedProjectDatabaseRead(projectKey).getResult().track(sharedProjectTracker, projectKey)

    private val taskTracker = Tracker<TaskKey.Root>()
    override fun getRootTaskObservable(taskKey: TaskKey.Root) =
        TaskDatabaseRead(taskKey).getResult().track(taskTracker, taskKey)

    fun getUsersObservable() = UsersDatabaseRead().getResult()

    sealed class LoadState<T : Any> {

        class Initial<T : Any> : LoadState<T>()

        class Loaded<T : Any>(val value: T) : LoadState<T>()
    }

    private class Tracker<KEY : Any> { // todo scheduling

        val keys = Collections.synchronizedSet(mutableSetOf<KEY>())
    }

    private fun <KEY : Any, T : Any> Observable<T>.track(tracker: Tracker<KEY>, key: KEY) = doOnSubscribe {
        check(key !in tracker.keys)

        tracker.keys += key
    }.doOnDispose {
        check(key in tracker.keys)

        tracker.keys -= key
    }

    override fun checkTrackers(
        userKeys: Set<UserKey>,
        privateProjectKeys: Set<ProjectKey.Private>,
        sharedProjectKeys: Set<ProjectKey.Shared>,
        taskKeys: Set<TaskKey.Root>,
    ) {
        fun <T : Any> check(loaded: Set<T>, tracker: Tracker<T>) = (loaded - tracker.keys).let {
            check(it.isEmpty()) { "missing: $it" }
        }

        check(userKeys, userTracker)
        check(privateProjectKeys, privateProjectTracker)
        check(sharedProjectKeys, sharedProjectTracker)
        check(taskKeys, taskTracker)
    }
}
