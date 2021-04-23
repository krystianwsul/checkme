package com.krystianwsul.checkme.firebase

import com.krystianwsul.checkme.firebase.factories.FriendsFactory
import com.krystianwsul.checkme.firebase.loaders.FriendsLoader
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.TaskKey
import com.krystianwsul.common.utils.UserKey
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single

class CustomTimeCoordinator(
        private val myUserKey: UserKey,
        private val friendsLoader: FriendsLoader,
        private val friendsFactorySingle: Single<FriendsFactory>,
) {

    fun getCustomTimes(projectKey: ProjectKey.Shared, foreignUserKeys: Set<UserKey>): Single<FriendsFactory> {
        check(myUserKey !in foreignUserKeys)

        if (foreignUserKeys.isNotEmpty()) friendsLoader.userKeyStore.requestCustomTimeUsers(projectKey, foreignUserKeys)

        return friendsFactorySingle.flatMap { friendsFactory ->
            Observable.just(Unit)
                    .concatWith(friendsFactory.changeTypes.map { })
                    .filter { friendsFactory.hasUserKeys(foreignUserKeys) }
                    .firstOrError()
                    .map { friendsFactory }
        }
    }

    fun getCustomTimes(taskKey: TaskKey.Root, foreignUserKeys: Set<UserKey>): Single<FriendsFactory> {
        check(myUserKey !in foreignUserKeys)

        if (foreignUserKeys.isNotEmpty()) friendsLoader.userKeyStore.requestCustomTimeUsers(taskKey, foreignUserKeys)

        return friendsFactorySingle.flatMap { friendsFactory ->
            Observable.just(Unit)
                    .concatWith(friendsFactory.changeTypes.map { })
                    .filter { friendsFactory.hasUserKeys(foreignUserKeys) }
                    .firstOrError()
                    .map { friendsFactory }
        }
    }
}