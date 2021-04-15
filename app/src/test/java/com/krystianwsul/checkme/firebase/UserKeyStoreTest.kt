package com.krystianwsul.checkme.firebase

import com.jakewharton.rxrelay3.PublishRelay
import com.krystianwsul.common.firebase.ChangeType
import com.krystianwsul.common.firebase.ChangeWrapper
import com.krystianwsul.common.utils.UserKey
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.observers.TestObserver
import org.junit.After
import org.junit.Before
import org.junit.Test

class UserKeyStoreTest {

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

        myUserChangeWrapperRelay.accept(ChangeWrapper(ChangeType.LOCAL, setOf(UserKey("1"))))
        testObserver.assertValueAt(
                1,
                ChangeWrapper(
                        ChangeType.LOCAL,
                        mapOf(UserKey("1") to UserKeyStore.LoadUserData.Friend(null))
                ),
        )
    }
}