package com.krystianwsul.checkme.gui.friends.findfriend.viewmodel

import com.github.tamir7.contacts.Contacts
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.parcelize.Parcelize

sealed class ContactsState : ViewModelState {

    abstract val viewState: FindFriendViewModel.ViewState

    open val nextStateSingle: Single<ContactsState>? = null

    abstract override fun toSerializableState(): SerializableState

    open fun processViewAction(viewAction: FindFriendViewModel.ViewAction): ContactsState? = null

    object Initial : ContactsState() {

        override val viewState get() = FindFriendViewModel.ViewState.Permissions

        override fun toSerializableState() = SerializableState.Initial

        override fun processViewAction(viewAction: FindFriendViewModel.ViewAction): ContactsState? {
            return when (viewAction) {
                is FindFriendViewModel.ViewAction.Permissions -> if (viewAction.granted) Loading else Denied
                else -> super.processViewAction(viewAction)
            }
        }
    }

    object Denied : ContactsState() {

        override val viewState get() = FindFriendViewModel.ViewState.Loaded(listOf())

        override fun toSerializableState() = SerializableState.Denied
    }

    object Loading : ContactsState() {

        override val viewState get() = FindFriendViewModel.ViewState.Loading

        override fun toSerializableState() = SerializableState.Loading

        override val nextStateSingle: Single<ContactsState>
            get() {
                return Single.fromCallable {
                    Contacts.getQuery()
                            .find()
                            .flatMap {
                                it.emails.map { email ->
                                    FindFriendViewModel.Contact(it.displayName, email.address, it.photoUri, null)
                                }
                            }
                }
                        .subscribeOn(Schedulers.io())
                        .map(ContactsState::Loaded)
                        .cast(ContactsState::class.java)
                        .observeOn(AndroidSchedulers.mainThread())
            }
    }

    data class Loaded(val contacts: List<FindFriendViewModel.Contact>) : ContactsState() {

        override val viewState get() = FindFriendViewModel.ViewState.Loaded(contacts)

        override fun toSerializableState() = SerializableState.Loaded(contacts)
    }

    sealed class SerializableState : ViewModelState.SerializableState {

        abstract override fun toState(): ContactsState

        @Parcelize
        object Initial : SerializableState() {

            override fun toState() = ContactsState.Initial
        }

        @Parcelize
        object Denied : SerializableState() {

            override fun toState() = ContactsState.Denied
        }

        @Parcelize
        object Loading : SerializableState() {

            override fun toState() = ContactsState.Loading
        }

        @Parcelize
        data class Loaded(val contacts: List<FindFriendViewModel.Contact>) : SerializableState() {

            override fun toState() = ContactsState.Loaded(contacts)
        }
    }
}