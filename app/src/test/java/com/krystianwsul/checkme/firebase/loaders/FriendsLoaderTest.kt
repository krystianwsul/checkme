package com.krystianwsul.checkme.firebase.loaders

import com.jakewharton.rxrelay3.PublishRelay
import com.krystianwsul.checkme.firebase.dependencies.UserKeyStore
import com.krystianwsul.checkme.firebase.snapshot.Snapshot
import com.krystianwsul.common.firebase.ChangeType
import com.krystianwsul.common.firebase.ChangeWrapper
import com.krystianwsul.common.firebase.DatabaseCallback
import com.krystianwsul.common.firebase.DomainThreadChecker
import com.krystianwsul.common.firebase.json.users.UserWrapper
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.TaskKey
import com.krystianwsul.common.utils.UserKey
import io.mockk.mockk
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import org.junit.After
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test

class FriendsLoaderTest {

    companion object {

        @JvmStatic
        @BeforeClass
        fun beforeClass() {
            DomainThreadChecker.instance = mockk(relaxed = true)
        }
    }

    class TestFriendsProvider : FriendsProvider {

        override val database = TestDatabase()

        class TestDatabase : FriendsProvider.Database() {

            private val userObservables = mutableMapOf<UserKey, PublishRelay<Snapshot<UserWrapper>>>()

            fun acceptUser(
                userKey: UserKey,
                userWrapper: UserWrapper,
            ) = userObservables.getValue(userKey).accept(Snapshot(userKey.key, userWrapper))

            override fun getUserObservable(userKey: UserKey): Observable<Snapshot<UserWrapper>> {
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

            override fun checkTrackers(
                userKeys: Set<UserKey>,
                privateProjectKeys: Set<ProjectKey.Private>,
                sharedProjectKeys: Set<ProjectKey.Shared>,
                taskKeys: Set<TaskKey.Root>,
            ) = Unit
        }
    }

    private val compositeDisposable = CompositeDisposable()

    private lateinit var rxErrorChecker: RxErrorChecker

    private lateinit var friendsKeysRelay: PublishRelay<Set<UserKey>>
    private lateinit var userKeyStore: UserKeyStore
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

        userKeyStore = UserKeyStore(
            friendsKeysRelay.map { ChangeWrapper(ChangeType.REMOTE, it) },
            compositeDisposable,
            mockk(relaxed = true),
        )

        friendsLoader = FriendsLoader(userKeyStore, compositeDisposable, friendsProvider)

        initialFriendsEmissionChecker =
            EmissionChecker("initialFriends", compositeDisposable, friendsLoader.initialFriendsEvent)
        addChangeFriendEmissionChecker =
            EmissionChecker("addChangeFriend", compositeDisposable, friendsLoader.addChangeFriendEvents)
        removeFriendsEmissionChecker =
            EmissionChecker("removeFriends", compositeDisposable, friendsLoader.removeFriendEvents)
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

    @Test
    fun testAddRemoveFriendTwice() {
        initialFriendsEmissionChecker.checkOne {
            friendsKeysRelay.accept(setOf())
        }

        friendsKeysRelay.accept(setOf(friendKey1))
        addChangeFriendEmissionChecker.checkOne {
            friendsProvider.database.acceptUser(friendKey1, UserWrapper())
        }

        removeFriendsEmissionChecker.checkOne {
            friendsKeysRelay.accept(setOf())
        }

        friendsKeysRelay.accept(setOf(friendKey1))
        addChangeFriendEmissionChecker.checkOne {
            friendsProvider.database.acceptUser(friendKey1, UserWrapper())
        }

        removeFriendsEmissionChecker.checkOne {
            friendsKeysRelay.accept(setOf())
        }
    }
}