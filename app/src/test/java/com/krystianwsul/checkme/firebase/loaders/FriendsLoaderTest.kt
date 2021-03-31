package com.krystianwsul.checkme.firebase.loaders

import com.jakewharton.rxrelay3.PublishRelay
import com.krystianwsul.checkme.firebase.loaders.snapshot.Snapshot
import com.krystianwsul.common.firebase.ChangeType
import com.krystianwsul.common.firebase.ChangeWrapper
import com.krystianwsul.common.firebase.DatabaseCallback
import com.krystianwsul.common.firebase.json.UserWrapper
import com.krystianwsul.common.utils.UserKey
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import org.junit.After
import org.junit.Before
import org.junit.Test

@ExperimentalStdlibApi
class FriendsLoaderTest {

    private class TestFriendsProvider : FriendsProvider {

        override val database = TestDatabase()

        class TestDatabase : FriendsProvider.Database() {

            private val userObservables = mutableMapOf<UserKey, PublishRelay<Snapshot>>()

            fun acceptUser(
                    userKey: UserKey,
                    userWrapper: UserWrapper
            ) = userObservables.getValue(userKey).accept(ValueTestSnapshot(userWrapper, userKey.key))

            override fun getRootInstanceObservable(taskFirebaseKey: String): Observable<Snapshot> {
                TODO("Not yet implemented")
            }

            override fun getUserObservable(userKey: UserKey): Observable<Snapshot> {
                if (!userObservables.containsKey(userKey))
                    userObservables[userKey] = PublishRelay.create()
                return userObservables.getValue(userKey)
            }

            override fun getNewId(path: String): String {
                TODO("Not yet implemented")
            }

            override fun update(values: Map<String, Any?>, callback: DatabaseCallback) {
                TODO("Not yet implemented")
            }
        }
    }

    private val compositeDisposable = CompositeDisposable()

    private lateinit var rxErrorChecker: RxErrorChecker

    private lateinit var friendsKeysRelay: PublishRelay<Set<UserKey>>
    private lateinit var friendsProvider: TestFriendsProvider

    private lateinit var friendsLoader: FriendsLoader

    private lateinit var initialFriendsEmissionChecker: EmissionChecker<FriendsLoader.InitialFriendsEvent>
    private lateinit var addChangeFriendEmissionChecker: EmissionChecker<FriendsLoader.AddChangeFriendEvent>
    private lateinit var removeFriendsEmissionChecker: EmissionChecker<FriendsLoader.RemoveFriendsEvent>

    private val friendKey1 = UserKey("friendKey1")
    private val friendKey2 = UserKey("friendKey2")

    @Before
    fun before() {
        rxErrorChecker = RxErrorChecker()

        friendsKeysRelay = PublishRelay.create()
        friendsProvider = TestFriendsProvider()

        friendsLoader = FriendsLoader(
                friendsKeysRelay.map { ChangeWrapper(ChangeType.LOCAL, it) },
                compositeDisposable,
                friendsProvider
        )

        initialFriendsEmissionChecker = EmissionChecker("initialFriends", compositeDisposable, friendsLoader.initialFriendsEvent)
        addChangeFriendEmissionChecker = EmissionChecker("addChangeFriend", compositeDisposable, friendsLoader.addChangeFriendEvents)
        removeFriendsEmissionChecker = EmissionChecker("removeFriends", compositeDisposable, friendsLoader.removeFriendEvents)
    }

    @After
    fun after() {
        compositeDisposable.clear()

        initialFriendsEmissionChecker.checkEmpty()
        addChangeFriendEmissionChecker.checkEmpty()
        removeFriendsEmissionChecker.checkEmpty()

        rxErrorChecker.check()
    }

    @Test
    fun testInitial() {
        friendsKeysRelay.accept(setOf(friendKey1))

        initialFriendsEmissionChecker.checkOne {
            friendsProvider.database.acceptUser(friendKey1, UserWrapper())
        }
    }

    @Test
    fun testAddFriend() {
        testInitial()

        friendsKeysRelay.accept(setOf(friendKey1, friendKey2))

        addChangeFriendEmissionChecker.checkOne {
            friendsProvider.database.acceptUser(friendKey2, UserWrapper())
        }
    }

    @Test
    fun testChangeInitialFriend() {
        testInitial()

        addChangeFriendEmissionChecker.checkOne {
            friendsProvider.database.acceptUser(friendKey1, UserWrapper())
        }
    }

    @Test
    fun testChangeAddedFriend() {
        testInitial()

        friendsKeysRelay.accept(setOf(friendKey1, friendKey2))

        addChangeFriendEmissionChecker.checkOne {
            friendsProvider.database.acceptUser(friendKey2, UserWrapper())
        }

        addChangeFriendEmissionChecker.checkOne {
            friendsProvider.database.acceptUser(friendKey2, UserWrapper())
        }
    }

    @Test
    fun testRemoveFriend() {
        testInitial()

        removeFriendsEmissionChecker.checkOne {
            friendsKeysRelay.accept(setOf())
        }
    }
}