package com.krystianwsul.checkme.gui.friends.findfriend.viewmodel

import android.os.Parcelable
import com.github.tamir7.contacts.Contacts
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.parcelize.Parcelize

sealed class ContactsState : Parcelable {

    abstract val viewState: FindFriendViewModel.ViewState

    open val nextStateSingle: Single<ContactsState>? = null

    open fun processViewAction(viewAction: FindFriendViewModel.ViewAction): ContactsState? = null

    @Parcelize
    object Permissions : ContactsState() {

        override val viewState get() = FindFriendViewModel.ViewState.Permissions

        override fun processViewAction(viewAction: FindFriendViewModel.ViewAction): ContactsState? {
            return when (viewAction) {
                is FindFriendViewModel.ViewAction.Permissions -> if (viewAction.granted) Loading else Denied
                else -> super.processViewAction(viewAction)
            }
        }
    }

    @Parcelize
    object Denied : ContactsState() {

        override val viewState get() = FindFriendViewModel.ViewState.Loaded(listOf())
    }

    @Parcelize
    object Loading : ContactsState() {

        override val viewState get() = FindFriendViewModel.ViewState.Loading

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

    @Parcelize
    data class Loaded(val contacts: List<FindFriendViewModel.Contact>) : ContactsState() {

        override val viewState get() = FindFriendViewModel.ViewState.Loaded(contacts)
    }
}