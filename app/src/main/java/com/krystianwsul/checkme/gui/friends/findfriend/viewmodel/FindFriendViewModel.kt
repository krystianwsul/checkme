package com.krystianwsul.checkme.gui.friends.findfriend.viewmodel

import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.github.tamir7.contacts.Contacts
import com.jakewharton.rxrelay3.BehaviorRelay
import com.jakewharton.rxrelay3.PublishRelay
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.firebase.AndroidDatabaseWrapper
import com.krystianwsul.checkme.utils.toV3
import com.krystianwsul.common.firebase.UserData
import com.krystianwsul.common.firebase.json.UserWrapper
import com.krystianwsul.common.utils.singleOrEmpty
import com.victorrendina.rxqueue2.QueueRelay
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.parcelize.Parcelize

class FindFriendViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {

    companion object {

        private const val KEY_STATE = "state"
    }

    private val clearedDisposable = CompositeDisposable()

    private val stateRelay = BehaviorRelay.createDefault(
            savedStateHandle[KEY_STATE] ?: State(ContactsState.Permissions, SearchState.None)
    )

    private val viewStateRelay = QueueRelay.create<ViewState>()

    init {
        stateRelay.map { it.viewState }
                .distinctUntilChanged()
                .subscribe(viewStateRelay::accept)
                .addTo(clearedDisposable)
    }

    val viewStateObservable = viewStateRelay.toV3()

    val viewActionRelay = PublishRelay.create<ViewAction>()!!

    init {
        clearedDisposable += stateRelay.subscribe { savedStateHandle[KEY_STATE] = it }

        stateRelay.switchMapSingle { it.nextStateSingle }
                .subscribe(stateRelay::accept)
                .addTo(clearedDisposable)

        stateRelay.switchMapSingle { viewActionRelay.firstOrError().map { viewAction -> it to viewAction } }
                .map { (state, viewAction) -> state.processViewAction(viewAction) }
                .subscribe(stateRelay::accept)
                .addTo(clearedDisposable)
    }

    override fun onCleared() = clearedDisposable.dispose()

    @Parcelize
    private data class State(val contactsState: ContactsState, val searchState: SearchState) : Parcelable {

        val viewState: ViewState
            get() {
                val contactsViewState = contactsState.viewState
                val searchViewState = searchState.viewState
                val viewStates = listOf(searchViewState, contactsViewState)

                viewStates.filterIsInstance<ViewState.Error>()
                        .singleOrEmpty()
                        ?.let { return it }

                viewStates.filterIsInstance<ViewState.Permissions>()
                        .singleOrEmpty()
                        ?.let { return it }

                viewStates.filterIsInstance<ViewState.Loading>()
                        .firstOrNull()
                        ?.let { return it }

                return viewStates.map { it as ViewState.Loaded }.let {
                    ViewState.Loaded(
                            it.map { it.contacts }
                                    .flatten()
                                    .distinctBy { it.email }
                    )
                }
            }

        val nextStateSingle: Single<State>
            get() {
                val nextContactsStateSingle = contactsState.nextStateSingle
                val nextSearchStateSingle = searchState.nextStateSingle

                check((nextContactsStateSingle == null) || (nextSearchStateSingle == null))

                return nextContactsStateSingle?.map { State(it, searchState) }
                        ?: nextSearchStateSingle?.map { State(contactsState, it) }
                        ?: Single.never()
            }

        fun processViewAction(viewAction: ViewAction): State {
            val nextContactsState = contactsState.processViewAction(viewAction)
            val nextSearchState = searchState.processViewAction(viewAction)

            return if (nextContactsState != null) {
                check(nextSearchState == null)

                State(nextContactsState, searchState)
            } else {
                checkNotNull(nextSearchState)

                State(contactsState, nextSearchState)
            }
        }
    }

    private sealed class ContactsState : Parcelable {

        abstract val viewState: ViewState

        open val nextStateSingle: Single<ContactsState>? = null

        open fun processViewAction(viewAction: ViewAction): ContactsState? = null

        @Parcelize
        object Permissions : ContactsState() {

            override val viewState get() = ViewState.Permissions

            override fun processViewAction(viewAction: ViewAction): ContactsState? {
                return when (viewAction) {
                    is ViewAction.Permissions -> if (viewAction.granted) Loading else Denied
                    else -> super.processViewAction(viewAction)
                }
            }
        }

        @Parcelize
        object Denied : ContactsState() {

            override val viewState get() = ViewState.Loaded(listOf())
        }

        @Parcelize
        object Loading : ContactsState() {

            override val viewState get() = ViewState.Loading

            override val nextStateSingle: Single<ContactsState>
                get() {
                    return Single.fromCallable {
                        Contacts.getQuery()
                                .find()
                                .flatMap {
                                    it.emails.map { email ->
                                        Contact(it.displayName, email.address, it.photoUri, null)
                                    }
                                }
                    }
                            .subscribeOn(Schedulers.io())
                            .map(ContactsState::Loaded)
                            .cast(ContactsState::class.java)
                            .observeOn(AndroidSchedulers.mainThread())
                }
        }

        @Parcelize
        data class Loaded(val contacts: List<Contact>) : ContactsState() {

            override val viewState get() = ViewState.Loaded(contacts)
        }
    }

    private sealed class SearchState : Parcelable {

        abstract val viewState: ViewState

        open val nextStateSingle: Single<SearchState>? = null

        open fun processViewAction(viewAction: ViewAction): SearchState? = null

        @Parcelize
        object None : SearchState() {

            override val viewState get() = ViewState.Loaded(listOf())

            override fun processViewAction(viewAction: ViewAction): SearchState? {
                return when (viewAction) {
                    is ViewAction.Search -> Loading(viewAction.email)
                    else -> super.processViewAction(viewAction)
                }
            }
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
                            if (it.exists())
                                Found(it.getValue(UserWrapper::class.java)!!)
                            else
                                Error(R.string.userNotFound, None)
                        }!!
        }

        @Parcelize
        data class Found(val userWrapper: UserWrapper) : SearchState() {

            override val viewState
                get() =
                    userWrapper.userData.run { ViewState.Loaded(listOf(Contact(name, email, photoUrl, userWrapper))) }

            override fun processViewAction(viewAction: ViewAction): SearchState? {
                return when (viewAction) {
                    is ViewAction.Search -> Loading(viewAction.email)
                    else -> super.processViewAction(viewAction)
                }
            }
        }

        @Parcelize
        data class Error(@StringRes private val stringRes: Int, private val nextState: SearchState) : SearchState() {

            override val viewState get() = ViewState.Error(stringRes)

            override val nextStateSingle get() = Single.just(nextState)!!
        }
    }

    @Parcelize
    data class Contact(
            val displayName: String,
            val email: String,
            val photoUri: String?,
            val userWrapper: UserWrapper?,
    ) : Parcelable

    sealed class ViewState {

        data class Error(@StringRes val stringRes: Int) : ViewState()

        object Permissions : ViewState()

        object Loading : ViewState()

        data class Loaded(val contacts: List<Contact>) : ViewState()
    }

    sealed class ViewAction {

        data class Permissions(val granted: Boolean) : ViewAction()

        data class Search(val email: String) : ViewAction()
    }
}