package com.krystianwsul.checkme.gui.friends.findfriend.viewmodel

import com.krystianwsul.common.utils.normalized
import io.reactivex.rxjava3.kotlin.merge
import kotlinx.parcelize.Parcelize

data class FindFriendState(
        val contactsState: ContactsState,
        val searchState: SearchState,
        private val query: String,
) : ViewModelState<FindFriendViewEvent, FindFriendViewModel> {

    fun getViewState(): FindFriendViewState {
        val (searchLoading, userWrappers) = when (searchState) {
            is SearchState.Loading -> true to listOf()
            is SearchState.Loaded -> false to searchState.userWrappers
        }

        val (contactsLoading, phoneContacts) = when (contactsState) {
            is ContactsState.Initial -> return FindFriendViewState.Permissions
            is ContactsState.Waiting -> false to listOf()
            is ContactsState.Denied -> false to listOf()
            is ContactsState.Loading -> true to listOf()
            is ContactsState.Loaded -> false to contactsState.contacts
        }

        if (searchLoading && contactsLoading) return FindFriendViewState.Loading

        val searchContacts = userWrappers.map {
            FindFriendViewModel.Contact(it.userData.name, it.userData.email, it.userData.photoUrl, it)
        }

        val normalizedQuery = query.normalized()
        fun List<FindFriendViewModel.Contact>.filterQuery() = if (normalizedQuery.isEmpty()) {
            this
        } else {
            filter { listOf(it.displayName, it.email).any { it.normalized().contains(normalizedQuery) } }
        }

        return FindFriendViewState.Loaded(
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

    override fun processViewAction(viewEvent: FindFriendViewEvent): FindFriendState {
        val newQuery = (viewEvent as? FindFriendViewEvent.Search)?.query ?: query

        return FindFriendState(
                contactsState.processViewAction(viewEvent),
                searchState.processViewAction(viewEvent),
                newQuery
        )
    }

    @Parcelize
    data class SerializableState(
            private val contactsState: ContactsState.SerializableState,
            private val searchState: SearchState.SerializableState,
            private val query: String,
    ) : ViewModelState.SerializableState<FindFriendViewEvent, FindFriendViewModel> {

        override fun toState(viewModel: FindFriendViewModel) =
                FindFriendState(contactsState.toState(viewModel), searchState.toState(viewModel), query)
    }
}