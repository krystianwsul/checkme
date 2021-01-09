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
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.UserKey
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
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

    private val stateQueue = RxQueue<SearchState>(savedStateHandle[KEY_STATE] ?: SearchState.None)

    private var state
        get() = stateQueue.value // todo friend need getter?
        private set(value) {
            stateQueue.accept(value)
        }

    private val stateObservable = stateQueue.share()!!

    val viewStateObservable = stateObservable.map { it.viewState }.distinctUntilChanged()!!

    init {
        clearedDisposable += stateObservable.subscribe { savedStateHandle[KEY_STATE] = it }

        stateObservable.switchMapSingle { it.nextStateSingle }
                .subscribe(stateQueue::accept)
                .addTo(clearedDisposable)
    }

    fun startSearch(email: String) {
        if (email.isEmpty()) return

        state = SearchState.Loading(email)
    }

    fun addFriend() {
        (state as SearchState.Found).apply {
            DomainFactory.instance.addFriend(SaveService.Source.GUI, userKey, userWrapper)
        }
    }

    fun fetchContacts() {
        Single.fromCallable {
            val x = ExactTimeStamp.Local.now
            Contacts.getQuery()
                    .find()
                    .flatMap { it.emails.map { email -> Contact(it.displayName, email.address, it.photoUri) } }
                    .also {
                        val y = ExactTimeStamp.Local.now

                        Log.e("asdf", "magic contacts time: " + (y.long - x.long))
                    }
        }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy { it.forEach { Log.e("asdf", "magic $it") } }
                .addTo(clearedDisposable)
    }

    override fun onCleared() {
        clearedDisposable.dispose()
    }

    private sealed class ContactsState : Parcelable {

        @Parcelize
        object Permissions : ContactsState()

        @Parcelize
        object Denied : ContactsState()

        @Parcelize
        object Loading : ContactsState()

        @Parcelize
        data class Loaded(val contacts: List<Contact>) : ContactsState()
    }

    private sealed class SearchState : Parcelable {

        abstract val viewState: ViewState

        open val nextStateSingle: Single<SearchState> = Single.never()

        @Parcelize
        object None : SearchState() {

            override val viewState get() = ViewState.None
        }

        @Parcelize
        data class Loading(val email: String) : SearchState() {

            init {
                check(email.isNotEmpty())
            }

            override val viewState get() = ViewState.Loading

            override val nextStateSingle
                get() = AndroidDatabaseWrapper.getUserObservable(UserData.getKey(email))
                        .firstOrError()
                        .map {
                            Log.e("asdf", "magic snapshot")
                            if (it.exists()) {
                                Found(UserKey(it.key), it.getValue(UserWrapper::class.java)!!)
                            } else {
                                Error(R.string.userNotFound, None)
                            }
                        }!!
        }

        @Parcelize
        data class Found(val userKey: UserKey, val userWrapper: UserWrapper) : SearchState() {

            override val viewState get() = ViewState.List(userKey, userWrapper)
        }

        @Parcelize
        data class Error(@StringRes private val stringRes: Int, private val nextState: SearchState) : SearchState() {

            override val viewState get() = ViewState.Error(stringRes)

            override val nextStateSingle get() = Single.just(nextState)!!
        }
    }

    @Parcelize
    data class Contact(val displayName: String, val email: String, val photoUri: String?) : Parcelable

    sealed class ViewState {

        object None : ViewState() // todo friends remove once contacts are added

        object Loading : ViewState()

        data class List(val userKey: UserKey, val userWrapper: UserWrapper) : ViewState()

        data class Error(@StringRes val stringRes: Int) : ViewState()
    }
}