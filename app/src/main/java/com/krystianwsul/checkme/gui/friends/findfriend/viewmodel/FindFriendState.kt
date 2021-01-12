package com.krystianwsul.checkme.gui.friends.findfriend.viewmodel

import com.krystianwsul.common.utils.normalized
import io.reactivex.rxjava3.kotlin.merge
import kotlinx.parcelize.Parcelize

data class FindFriendState(
        val contactsState: ContactsState,
        val searchState: SearchState,
        private val query: String,
) : ViewModelState<FindFriendViewModel.ViewAction> {

    fun getViewState(): FindFriendViewModel.ViewState {
        val (searchLoading, userWrappers) = when (searchState) {
            is SearchState.Loading -> true to listOf()
            is SearchState.Loaded -> false to searchState.userWrappers
        }

        val (contactsLoading, phoneContacts) = when (contactsState) {
            is ContactsState.Initial -> return FindFriendViewModel.ViewState.Permissions
            is ContactsState.Waiting -> false to listOf()
            is ContactsState.Denied -> false to listOf()
            is ContactsState.Loading -> true to listOf()
            is ContactsState.Loaded -> false to contactsState.contacts
        }

        if (searchLoading && contactsLoading) return FindFriendViewModel.ViewState.Loading

        val searchContacts = userWrappers.map {
            FindFriendViewModel.Contact(it.userData.name, it.userData.email, it.userData.photoUrl, it)
        }

        val normalizedQuery = query.normalized()
        fun List<FindFriendViewModel.Contact>.filterQuery() = if (normalizedQuery.isEmpty()) {
            this
        } else {
            filter { listOf(it.displayName, it.email).any { it.normalized().contains(normalizedQuery) } }
        }

        return FindFriendViewModel.ViewState.Loaded(
                (searchContacts + phoneContacts).distinctBy { it.email }
                        .filterQuery()
                        .sortedBy { it.displayName },
                searchLoading || contactsLoading
        )
    }

    override val nextStateSingle = listOf(
            contactsState.nextStateSingle.map { FindFriendState(it, searchState, query) },
            searchState.nextStateSingle.map { FindFriendState(contactsState, it, query) },
    ).map { it.toObservable() }
            .merge()
            .firstOrError()!!

    override fun toSerializableState(): SerializableState? {
        val contactsSerializableState = contactsState.toSerializableState()
        val searchSerializableState = searchState.toSerializableState()

        return if (contactsSerializableState != null && searchSerializableState != null)
            SerializableState(contactsSerializableState, searchSerializableState, query)
        else
            null
    }

    override fun processViewAction(viewAction: FindFriendViewModel.ViewAction): FindFriendState {
        val newQuery = (viewAction as? FindFriendViewModel.ViewAction.Search)?.email ?: query

        return FindFriendState(
                contactsState.processViewAction(viewAction),
                searchState.processViewAction(viewAction),
                newQuery
        )
    }

    @Parcelize
    data class SerializableState(
            private val contactsState: ContactsState.SerializableState,
            private val searchState: SearchState.SerializableState,
            private val query: String,
    ) : ViewModelState.SerializableState<FindFriendViewModel.ViewAction> {

        override fun toState() = FindFriendState(contactsState.toState(), searchState.toState(), query)
    }
}