package com.krystianwsul.checkme.firebase

import com.krystianwsul.checkme.firebase.factories.FriendsFactory
import com.krystianwsul.common.utils.UserKey
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single

class CustomTimeCoordinator(private val myUserKey: UserKey, private val friendsFactorySingle: Single<FriendsFactory>) {

    fun getCustomTimes(foreignUserKeys: Set<UserKey>): Single<FriendsFactory> {
        check(myUserKey !in foreignUserKeys)

        return friendsFactorySingle.flatMap { friendsFactory ->
            Observable.just(Unit)
                    .concatWith(friendsFactory.changeTypes.map { })
                    .filter { friendsFactory.hasUserKeys(foreignUserKeys) }
                    .firstOrError()
                    .map { friendsFactory }
        }
    }
}