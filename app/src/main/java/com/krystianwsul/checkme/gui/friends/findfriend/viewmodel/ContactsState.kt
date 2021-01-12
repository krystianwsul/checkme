package com.krystianwsul.checkme.gui.friends.findfriend.viewmodel

import com.github.tamir7.contacts.Contacts
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.parcelize.Parcelize

sealed class ContactsState : ViewModelState<FindFriendViewModel.ViewAction> {

    override val nextStateSingle: Single<out ContactsState> = Single.never()

    abstract override fun toSerializableState(): SerializableState?

    override fun processViewAction(viewAction: FindFriendViewModel.ViewAction): ContactsState = this

    object Initial : ContactsState() {

        override val nextStateSingle = Single.just(Waiting)!!

        override fun toSerializableState(): SerializableState? = null
    }

    object Waiting : ContactsState() {

        override fun toSerializableState() = SerializableState.Initial

        override fun processViewAction(viewAction: FindFriendViewModel.ViewAction): ContactsState {
            return when (viewAction) {
                is FindFriendViewModel.ViewAction.Permissions -> if (viewAction.granted) Loading else Denied
                else -> super.processViewAction(viewAction)
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

    sealed class SerializableState : ViewModelState.SerializableState<FindFriendViewModel.ViewAction> {

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
        data class Loaded(val contacts: List<FindFriendViewModel.Contact>) : SerializableState() {

            override fun toState() = ContactsState.Loaded(contacts)
        }
    }
}