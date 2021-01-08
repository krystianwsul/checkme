package com.krystianwsul.checkme.viewmodels

import android.os.Parcelable
import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.github.tamir7.contacts.Contacts
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.extensions.addFriend
import com.krystianwsul.checkme.firebase.AndroidDatabaseWrapper
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.utils.NonNullRelayProperty
import com.krystianwsul.common.firebase.UserData
import com.krystianwsul.common.firebase.json.UserWrapper
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

    private val stateProperty = NonNullRelayProperty<State>(savedStateHandle[KEY_STATE] ?: State.None)
    private var state by stateProperty

    val stateObservable = stateProperty.observable.distinctUntilChanged()!!

    init {
        clearedDisposable += stateObservable.subscribe { savedStateHandle[KEY_STATE] = it }

        if (state is State.Loading) loadUser()
    }

    private var databaseReference: DatabaseReference? = null
    private var valueEventListener: ValueEventListener? = null

    fun startSearch(email: String) {
        if (email.isEmpty()) return

        disconnect()

        state = State.Loading(email)

        loadUser() // todo friend rx
    }

    fun addFriend() {
        (state as State.Found).apply {
            DomainFactory.instance.addFriend(SaveService.Source.GUI, userKey, userWrapper)
        }
    }

    private fun loadUser() {
        val key = UserData.getKey((state as State.Loading).email)

        check(valueEventListener == null)
        check(databaseReference == null)

        valueEventListener = object : ValueEventListener {

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                databaseReference!!.removeEventListener(valueEventListener!!)

                valueEventListener = null
                databaseReference = null

                if (dataSnapshot.exists()) {
                    state = State.Found(
                            UserKey(dataSnapshot.key!!),
                            dataSnapshot.getValue(UserWrapper::class.java)!!
                    )
                } else {
                    state = State.Error(R.string.userNotFound) // todo friend make queue
                    state = State.None
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                databaseReference!!.removeEventListener(valueEventListener!!)

                state = State.None

                valueEventListener = null
                databaseReference = null

                MyCrashlytics.logException(databaseError.toException())

                state = State.Error(R.string.connectionError)
                state = State.None
            }
        }

        databaseReference = AndroidDatabaseWrapper.getUserDataDatabaseReference(key)

        databaseReference!!.addValueEventListener(valueEventListener!!)
    }

    private fun disconnect() {
        if (state is State.Loading) { // todo friend rx
            checkNotNull(databaseReference)
            checkNotNull(valueEventListener)

            databaseReference!!.removeEventListener(valueEventListener!!)
        } else {
            check(databaseReference == null)
            check(valueEventListener == null)
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

        @Parcelize
        object None : State()

        @Parcelize
        data class Loading(val email: String) : State() {

            init {
                check(email.isNotEmpty())
            }
        }

        @Parcelize
        data class Found(val userKey: UserKey, val userWrapper: UserWrapper) : State()

        @Parcelize
        data class Error(@StringRes val stringRes: Int) : State()
    }

    data class Contact(val displayName: String, val email: String, val photoUri: String?)
}