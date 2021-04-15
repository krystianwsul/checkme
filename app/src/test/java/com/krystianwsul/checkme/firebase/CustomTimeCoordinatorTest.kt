package com.krystianwsul.checkme.firebase

import com.jakewharton.rxrelay3.PublishRelay
import com.krystianwsul.checkme.firebase.factories.FriendsFactory
import com.krystianwsul.checkme.firebase.loaders.FriendsLoader
import com.krystianwsul.checkme.firebase.loaders.FriendsLoaderTest
import com.krystianwsul.common.firebase.ChangeType
import com.krystianwsul.common.firebase.ChangeWrapper
import com.krystianwsul.common.utils.UserKey
import io.mockk.mockk
import io.reactivex.rxjava3.disposables.CompositeDisposable
import org.junit.After
import org.junit.Before
import org.junit.Test

class CustomTimeCoordinatorTest {

    companion object {

        private val myUserKey = UserKey("myUserKey")

        private val userKey1 = UserKey("1")
        private val userKey2 = UserKey("2")
        private val userKey3 = UserKey("3")
        private val userKey4 = UserKey("4")
        private val userKey5 = UserKey("5")
        private val userKey6 = UserKey("6")
    }

    private val domainDisposable = CompositeDisposable()

    private lateinit var friendKeysRelay: PublishRelay<Set<UserKey>>

    @Before
    fun before() {
        friendKeysRelay = PublishRelay.create()
    }

    @After
    fun after() {
        domainDisposable.clear()
    }

    @Test
    fun testEmptyEmitsImmediately() {
        val userKeyStore = UserKeyStore(
                friendKeysRelay.map { ChangeWrapper(ChangeType.REMOTE, it) },
                domainDisposable,
        )

        val friendsProvider = FriendsLoaderTest.TestFriendsProvider()

        val friendsLoader = FriendsLoader(userKeyStore, domainDisposable, friendsProvider)

        friendKeysRelay.accept(setOf())

        // This is copied from FactoryLoader.  Probably should be in a class or something.
        val friendsFactorySingle = friendsLoader.initialFriendsEvent.map {
            FriendsFactory(
                    friendsLoader,
                    it,
                    domainDisposable,
                    mockk(),
            )
        }

        val customTimeCoordinator = CustomTimeCoordinator(myUserKey, friendsLoader, friendsFactorySingle)

        val testObserver = customTimeCoordinator.observeCustomTimes(setOf()).test()
        testObserver.assertValueCount(1)
    }
}