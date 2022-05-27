package com.krystianwsul.checkme.firebase.loaders

import com.krystianwsul.checkme.firebase.factories.FriendsFactory
import com.krystianwsul.checkme.firebase.factories.MyUserFactory
import com.krystianwsul.checkme.firebase.factories.ProjectsFactory
import com.krystianwsul.checkme.firebase.roottask.RootTasksFactory
import com.krystianwsul.checkme.firebase.snapshot.Snapshot
import com.krystianwsul.checkme.utils.publishImmediate
import com.krystianwsul.common.firebase.ChangeType
import com.krystianwsul.common.firebase.json.users.UserWrapper
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.merge

class ChangeTypeSource(
    projectsFactorySingle: Single<ProjectsFactory>,
    friendsFactorySingle: Single<FriendsFactory>,
    userDatabaseRx: DatabaseRx<Snapshot<UserWrapper>>,
    userFactorySingle: Single<MyUserFactory>,
    rootTasksFactory: RootTasksFactory,
    domainDisposable: CompositeDisposable,
) {

    val changeTypes: Observable<ChangeType>

    init {
        val userFactoryChangeTypes = userDatabaseRx.changes.flatMapMaybe { snapshot ->
            userFactorySingle.filter { it.onNewSnapshot(snapshot) }
        }

        changeTypes = listOf(
            projectsFactorySingle.flatMapObservable { it.remoteChanges }.map { ChangeType.REMOTE },
            friendsFactorySingle.flatMapObservable { it.remoteChanges }.map { ChangeType.REMOTE },
            userFactoryChangeTypes.map { ChangeType.REMOTE },
            rootTasksFactory.changeTypes, // todo cleanup
        ).merge().publishImmediate(domainDisposable)
    }
}