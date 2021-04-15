package com.krystianwsul.checkme.firebase

import com.krystianwsul.common.firebase.ChangeWrapper
import com.krystianwsul.common.utils.UserKey
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.observers.TestObserver
import io.reactivex.rxjava3.subjects.PublishSubject
import org.junit.After
import org.junit.Before
import org.junit.Test

class UserKeyStoreTest {

    private lateinit var compositeDisposable: CompositeDisposable
    private lateinit var myUserChangeWrapperSubject: PublishSubject<ChangeWrapper<Set<UserKey>>>
    private lateinit var userKeyStore: UserKeyStore
    private lateinit var testObserver: TestObserver<ChangeWrapper<Map<UserKey, UserKeyStore.LoadUserData>>>

    @Before
    fun before() {
        compositeDisposable = CompositeDisposable()
        myUserChangeWrapperSubject = PublishSubject.create()
        userKeyStore = UserKeyStore(myUserChangeWrapperSubject, compositeDisposable)
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
}