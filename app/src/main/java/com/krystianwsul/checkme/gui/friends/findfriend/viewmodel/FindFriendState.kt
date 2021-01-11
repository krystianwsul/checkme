package com.krystianwsul.checkme.gui.friends.findfriend.viewmodel

import io.reactivex.rxjava3.kotlin.merge
import kotlinx.parcelize.Parcelize

data class FindFriendState(val contactsState: ContactsState, val searchState: SearchState) : ViewModelState {

    fun getViewState(): FindFriendViewModel.ViewState {
        val (searchLoading, userWrapper) = when (searchState) {
            is SearchState.Initial -> false to null
            is SearchState.Loading -> true to null
            is SearchState.Found -> false to searchState.userWrapper
            is SearchState.Error -> return FindFriendViewModel.ViewState.Error(searchState.stringRes)
        }

        val (contactsLoading, contacts) = when (contactsState) {
            is ContactsState.Initial -> return FindFriendViewModel.ViewState.Permissions
            is ContactsState.Waiting -> false to listOf()
            is ContactsState.Denied -> false to listOf()
            is ContactsState.Loading -> true to listOf()
            is ContactsState.Loaded -> false to contactsState.contacts
        }

        if (searchLoading && contactsLoading) return FindFriendViewModel.ViewState.Loading

        val searchContact = userWrapper?.let {
            FindFriendViewModel.Contact(it.userData.name, it.userData.email, it.userData.photoUrl, it)
        }

        return FindFriendViewModel.ViewState.Loaded(
                (listOfNotNull(searchContact) + contacts).distinctBy { it.email }
        )
    }

    override val nextStateSingle = listOf(
            contactsState.nextStateSingle.map { FindFriendState(it, searchState) },
            searchState.nextStateSingle.map { FindFriendState(contactsState, it) },
    ).map { it.toObservable() }
            .merge()
            .firstOrError()!!

    override fun toSerializableState(): SerializableState? {
        val contactsSerializableState = contactsState.toSerializableState()
        val searchSerializableState = searchState.toSerializableState()

        return if (contactsSerializableState != null && searchSerializableState != null)
            SerializableState(contactsSerializableState, searchSerializableState)
        else
            null
    }

    fun processViewAction(viewAction: FindFriendViewModel.ViewAction): FindFriendState { // todo friend fix this before testing
        val nextContactsState = contactsState.processViewAction(viewAction)
        val nextSearchState = searchState.processViewAction(viewAction)

        return if (nextContactsState != null) {
            check(nextSearchState == null)

            FindFriendState(nextContactsState, searchState)
        } else {
            checkNotNull(nextSearchState)

            FindFriendState(contactsState, nextSearchState)
        }
    }

    @Parcelize
    data class SerializableState(
            val contactsState: ContactsState.SerializableState,
            val searchState: SearchState.SerializableState,
    ) : ViewModelState.SerializableState {

        override fun toState() = FindFriendState(contactsState.toState(), searchState.toState())
    }
}