package com.krystianwsul.checkme.gui.friends.findfriend.viewmodel

import com.github.tamir7.contacts.Contacts
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.parcelize.Parcelize

sealed class ContactsState : ViewModelState<FindFriendViewEvent, FindFriendViewModel> {

    override val nextStateSingle: Single<out ContactsState> = Single.never()

    abstract override fun toSerializableState(): SerializableState?

    override fun processViewEvent(viewEvent: FindFriendViewEvent): ContactsState = this

    object Initial : ContactsState() {

        override val nextStateSingle = Single.just(WaitingForPermissions)!!

        override fun toSerializableState(): SerializableState? = null
    }

    object WaitingForPermissions : ContactsState() {

        override fun toSerializableState() = SerializableState.Initial

        override fun processViewEvent(viewEvent: FindFriendViewEvent): ContactsState {
            return when (viewEvent) {
                is FindFriendViewEvent.Permissions -> if (viewEvent.granted) Loading else Denied
                else -> super.processViewEvent(viewEvent)
            }
        }
    }

    object Denied : ContactsState() {

        override fun toSerializableState() = SerializableState.Denied
    }

    object Loading : ContactsState() {

        override fun toSerializableState() = SerializableState.Initial

        override val nextStateSingle = Single.fromCallable {
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
                .observeOn(AndroidSchedulers.mainThread())!!
    }

    data class Loaded(val contacts: List<FindFriendViewModel.Contact>) : ContactsState() {

        override fun toSerializableState() = SerializableState.Loaded(contacts)
    }

    sealed class SerializableState :
            ViewModelState.SerializableState<FindFriendViewEvent, FindFriendViewModel> {

        abstract override fun toState(viewModel: FindFriendViewModel): ContactsState

        @Parcelize
        object Initial : SerializableState() {

            override fun toState(viewModel: FindFriendViewModel) = ContactsState.Initial
        }

        @Parcelize
        object Denied : SerializableState() {

            override fun toState(viewModel: FindFriendViewModel) = ContactsState.Denied
        }

        @Parcelize
        data class Loaded(val contacts: List<FindFriendViewModel.Contact>) : SerializableState() {

            override fun toState(viewModel: FindFriendViewModel) = ContactsState.Loaded(contacts)
        }
    }
}