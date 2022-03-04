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

    override fun getUserObservable(userKey: UserKey) = UserDatabaseRead(userKey).getResult()

    override fun getPrivateProjectObservable(projectKey: ProjectKey.Private) =
        PrivateProjectDatabaseRead(projectKey).getResult()

    override fun getSharedProjectObservable(projectKey: ProjectKey.Shared) =
        SharedProjectDatabaseRead(projectKey).getResult()

    override fun getRootTaskObservable(taskKey: TaskKey.Root) = TaskDatabaseRead(taskKey).getResult()

    fun getUsersObservable() = UsersDatabaseRead().getResult()

    sealed class LoadState<T : Any> {

        class Initial<T : Any> : LoadState<T>()

        class Loaded<T : Any>(val value: T) : LoadState<T>()
    }
}
