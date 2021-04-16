package com.krystianwsul.checkme.firebase

import com.jakewharton.rxrelay3.PublishRelay
import com.krystianwsul.checkme.firebase.factories.FriendsFactory
import com.krystianwsul.checkme.firebase.loaders.FriendsLoader
import com.krystianwsul.checkme.firebase.loaders.FriendsLoaderTest
import com.krystianwsul.common.firebase.ChangeType
import com.krystianwsul.common.firebase.ChangeWrapper
import com.krystianwsul.common.firebase.DomainThreadChecker
import com.krystianwsul.common.firebase.json.UserWrapper
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.UserKey
import io.mockk.mockk
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import org.junit.After
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test

class CustomTimeCoordinatorTest {

    companion object {

        private val myUserKey = UserKey("myUserKey")

        private val userKey1 = UserKey("1")
        private val userKey2 = UserKey("2")

        private val projectKey1 = ProjectKey.Shared("1")

        @JvmStatic
        @BeforeClass
        fun beforeClass() {
            DomainThreadChecker.instance = mockk(relaxed = true)
        }
    }

    private val domainDisposable = CompositeDisposable()

    private lateinit var friendKeysRelay: PublishRelay<Set<UserKey>>

    private lateinit var userKeyStore: UserKeyStore
    private lateinit var friendsProvider: FriendsLoaderTest.TestFriendsProvider
    private lateinit var friendsLoader: FriendsLoader
    private lateinit var customTimeCoordinator: CustomTimeCoordinator

    @Before
    fun before() {
        friendKeysRelay = PublishRelay.create()

        userKeyStore = UserKeyStore(friendKeysRelay.map { ChangeWrapper(ChangeType.REMOTE, it) }, domainDisposable)
        friendsProvider = FriendsLoaderTest.TestFriendsProvider()
        friendsLoader = FriendsLoader(userKeyStore, domainDisposable, friendsProvider)

        // This is copied from FactoryLoader.  Probably should be in a class or something.
        val friendsFactorySingle = friendsLoader.initialFriendsEvent.map {
            FriendsFactory(
                    friendsLoader,
                    it,
                    domainDisposable,
                    mockk(),
            )
        }
                .cache()
                .apply { domainDisposable += subscribe() }

        customTimeCoordinator = CustomTimeCoordinator(myUserKey, friendsLoader, friendsFactorySingle)
    }

    @After
    fun after() {
        domainDisposable.clear()
    }

    @Test
    fun testEmptyEmitsImmediately() {
        friendKeysRelay.accept(setOf())

        val testObserver = customTimeCoordinator.observeCustomTimes(projectKey1, setOf()).test()
        testObserver.assertValueCount(1)
    }

    @Test
    fun testEmptyPresentFriendEmitsImmediately() {
        friendKeysRelay.accept(setOf(userKey1))
        friendsProvider.database.acceptUser(userKey1, UserWrapper())

        val testObserver = customTimeCoordinator.observeCustomTimes(projectKey1, setOf(userKey1)).test()
        testObserver.assertValueCount(1)
    }

    @Test
    fun testAbsentFriendEmitsAfterLoad() {
        friendKeysRelay.accept(setOf(userKey1))

        val testObserver = customTimeCoordinator.observeCustomTimes(projectKey1, setOf(userKey1)).test()
        testObserver.assertEmpty()

        friendsProvider.database.acceptUser(userKey1, UserWrapper())
        testObserver.assertValueCount(1)
    }

    @Test
    fun testStrangerEmitsAfterLoad() {
        friendKeysRelay.accept(setOf(userKey1))
        friendsProvider.database.acceptUser(userKey1, UserWrapper())

        val testObserver = customTimeCoordinator.observeCustomTimes(projectKey1, setOf(userKey2)).test()
        testObserver.assertEmpty()

        friendsProvider.database.acceptUser(userKey2, UserWrapper())
        testObserver.assertValueCount(1)
    }
}