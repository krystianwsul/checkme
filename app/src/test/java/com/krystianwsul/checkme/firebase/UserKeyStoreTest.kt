package com.krystianwsul.checkme.firebase

import com.jakewharton.rxrelay3.PublishRelay
import com.krystianwsul.common.firebase.ChangeType
import com.krystianwsul.common.firebase.ChangeWrapper
import com.krystianwsul.common.firebase.DomainThreadChecker
import com.krystianwsul.common.firebase.json.UserWrapper
import com.krystianwsul.common.firebase.records.RootUserRecord
import com.krystianwsul.common.utils.UserKey
import io.mockk.mockk
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.observers.TestObserver
import org.junit.After
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test

class UserKeyStoreTest {

    companion object {

        private val userKey1 = UserKey("1")
        private val userKey2 = UserKey("2")
        private val userKey3 = UserKey("3")
        private val userKey4 = UserKey("4")
        private val userKey5 = UserKey("5")
        private val userKey6 = UserKey("6")

        @JvmStatic
        @BeforeClass
        fun beforeClass() {
            DomainThreadChecker.instance = mockk(relaxed = true)
        }
    }

    private lateinit var compositeDisposable: CompositeDisposable
    private lateinit var myUserChangeWrapperRelay: PublishRelay<ChangeWrapper<Set<UserKey>>>
    private lateinit var userKeyStore: UserKeyStore
    private lateinit var testObserver: TestObserver<ChangeWrapper<Map<UserKey, UserKeyStore.LoadUserData>>>

    @Before
    fun before() {
        compositeDisposable = CompositeDisposable()
        myUserChangeWrapperRelay = PublishRelay.create()
        userKeyStore = UserKeyStore(myUserChangeWrapperRelay, compositeDisposable)
        testObserver = userKeyStore.loadUserDataObservable.test()
    }

    @After
    fun after() {
        compositeDisposable.clear()
    }

    @Test
    fun testInitial() {
        testObserver.assertEmpty()
    }

    @Test
    fun testMyUserChanges() {
        myUserChangeWrapperRelay.accept(ChangeWrapper(ChangeType.REMOTE, setOf()))
        testObserver.assertValue(ChangeWrapper(ChangeType.REMOTE, mapOf()))

        myUserChangeWrapperRelay.accept(ChangeWrapper(ChangeType.LOCAL, setOf(userKey1)))
        testObserver.assertValueAt(
                1,
                ChangeWrapper(
                        ChangeType.LOCAL,
                        mapOf(userKey1 to UserKeyStore.LoadUserData.Friend(null))
                ),
        )
    }

    private fun newUserRecord(userKey: UserKey) = RootUserRecord(mockk(), false, UserWrapper(), userKey)

    @Test
    fun testAddFriend() {
        // start with some dummy data from myUser
        myUserChangeWrapperRelay.accept(ChangeWrapper(ChangeType.REMOTE, setOf(userKey1)))
        val currentMap = mutableMapOf(userKey1 to UserKeyStore.LoadUserData.Friend(null))
        testObserver.assertValue(ChangeWrapper(ChangeType.REMOTE, currentMap))

        // add a friend
        val rootUserRecord2 = newUserRecord(userKey2)
        userKeyStore.addFriend(rootUserRecord2)

        currentMap[userKey2] =
                UserKeyStore.LoadUserData.Friend(UserKeyStore.AddFriendData(userKey2.key, rootUserRecord2.userWrapper))

        testObserver.assertValueAt(1, ChangeWrapper(ChangeType.LOCAL, currentMap))
    }

    @Test
    fun testAddFriendOverwriteWithRemoteChange() {
        testAddFriend()

        myUserChangeWrapperRelay.accept(ChangeWrapper(ChangeType.REMOTE, setOf(userKey3)))
        val currentMap = mutableMapOf(userKey3 to UserKeyStore.LoadUserData.Friend(null))
        testObserver.assertValueAt(2, ChangeWrapper(ChangeType.REMOTE, currentMap))

        val rootUserRecord4 = newUserRecord(userKey4)

        userKeyStore.addFriend(rootUserRecord4)
        currentMap[userKey4] =
                UserKeyStore.LoadUserData.Friend(UserKeyStore.AddFriendData(userKey4.key, rootUserRecord4.userWrapper))
        testObserver.assertValueAt(3, ChangeWrapper(ChangeType.LOCAL, currentMap))
    }

    /*
    @Test
    fun testRequestCustomTimesForNewUser() {
        // start with some dummy data from myUser
        myUserChangeWrapperRelay.accept(ChangeWrapper(ChangeType.REMOTE, setOf(userKey1)))
        val currentMap = mutableMapOf<UserKey, UserKeyStore.LoadUserData>(userKey1 to UserKeyStore.LoadUserData.Friend(null))
        testObserver.assertValue(ChangeWrapper(ChangeType.REMOTE, currentMap))

        userKeyStore.requestCustomTimeUsers(setOf(userKey2))
        currentMap[userKey2] = UserKeyStore.LoadUserData.CustomTimes
        testObserver.assertValueAt(1, ChangeWrapper(ChangeType.REMOTE, currentMap))
    }*/
}