package com.krystianwsul.checkme.viewmodels

import android.os.Parcelable
import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.github.tamir7.contacts.Contacts
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.extensions.addFriend
import com.krystianwsul.checkme.firebase.AndroidDatabaseWrapper
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.utils.RxQueue
import com.krystianwsul.common.firebase.UserData
import com.krystianwsul.common.firebase.json.UserWrapper
import com.krystianwsul.common.utils.UserKey
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.parcelize.Parcelize

class FindFriendViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {

    companion object {

        private const val KEY_STATE = "state"
    }

    private val clearedDisposable = CompositeDisposable()

    private val stateQueue = RxQueue<State>(savedStateHandle[KEY_STATE] ?: State.None)

    var state
        get() = stateQueue.value
        private set(value) {
            stateQueue.accept(value)
        }

    val stateObservable = stateQueue.distinctUntilChanged().share()!! // todo friend separate ViewState class

    init {
        clearedDisposable += stateObservable.subscribe { savedStateHandle[KEY_STATE] = it }

        stateObservable.switchMap { it.nextState }
                .subscribe(stateQueue::accept)
                .addTo(clearedDisposable)
    }

    fun startSearch(email: String) {
        if (email.isEmpty()) return

        state = State.Loading(email)
    }

    fun addFriend() {
        (state as State.Found).apply {
            DomainFactory.instance.addFriend(SaveService.Source.GUI, userKey, userWrapper)
        }
    }

    fun fetchContacts() {
        Single.fromCallable {
            Contacts.getQuery()
                    .find()
                    .flatMap { it.emails.map { email -> Contact(it.displayName, email.address, it.photoUri) } }
        }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy { it.forEach { Log.e("asdf", "magic $it") } }
                .addTo(clearedDisposable)
    }

    override fun onCleared() {
        clearedDisposable.dispose()
    }

    sealed class State : Parcelable {

        open val nextState: Observable<State> = Observable.never()

        @Parcelize
        object None : State()

        @Parcelize
        data class Loading(val email: String) : State() {

            init {
                check(email.isNotEmpty())
            }

            override val nextState
                get() = AndroidDatabaseWrapper.getUserObservable(UserData.getKey(email)).switchMap {
                    Log.e("asdf", "magic snapshot")
                    if (it.exists()) {
                        Observable.just(Found(UserKey(it.key), it.getValue(UserWrapper::class.java)!!))
                    } else {
                        Observable.just(Error(R.string.userNotFound), None)
                    }
                }!!
        }

        @Parcelize
        data class Found(val userKey: UserKey, val userWrapper: UserWrapper) : State()

        @Parcelize
        data class Error(@StringRes val stringRes: Int) : State()
    }

    data class Contact(val displayName: String, val email: String, val photoUri: String?)
}