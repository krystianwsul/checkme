package com.krystianwsul.checkme.gui.friends.findfriend.viewmodel

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.common.utils.normalized
import io.reactivex.rxjava3.kotlin.merge
import kotlinx.parcelize.Parcelize

data class FindFriendState(
        val contactsState: ContactsState,
        val databaseState: DatabaseState,
        private val query: String,
) : ModelState<FindFriendViewEvent, FindFriendViewModel> {

    fun getViewState(): FindFriendViewState {
        val (searchLoading, databasePeople) = when (databaseState) {
            is DatabaseState.Loading -> true to listOf()
            is DatabaseState.Loaded -> false to databaseState.userWrappers.map {
                FindFriendViewModel.Person(it.userData.name, it.userData.email, it.userData.photoUrl, it)
            }
        }

        val (contactsLoading, phonePeople) = when (contactsState) {
            is ContactsState.Initial -> return FindFriendViewState.Permissions
            is ContactsState.WaitingForPermissions -> false to listOf()
            is ContactsState.Denied -> false to listOf()
            is ContactsState.Loading -> true to listOf()
            is ContactsState.Loaded -> false to contactsState.people
        }

        if (searchLoading && contactsLoading) return FindFriendViewState.Loading

        val normalizedQuery = query.normalized()
        fun List<FindFriendViewModel.Person>.filterQuery() = if (normalizedQuery.isEmpty()) {
            this
        } else {
            filter { listOf(it.displayName, it.email).any { it.normalized().contains(normalizedQuery) } }
        }

        val userInfo = DomainFactory.instance
                .deviceDbInfo
                .userInfo

        return FindFriendViewState.Loaded(
                (databasePeople + phonePeople).distinctBy { it.email }
                        .filter { it.email != userInfo.email }
                        .filterQuery()
                        .sortedBy { it.displayName },
                searchLoading || contactsLoading
        )
    }

    override val nextStateSingle = listOf(
            contactsState.nextStateSingle.map { FindFriendState(it, databaseState, query) },
            databaseState.nextStateSingle.map { FindFriendState(contactsState, it, query) },
    ).map { it.toObservable() }
            .merge()
            .firstOrError()!!

    override fun toSerializableState(): SerializableState? {
        val contactsSerializableState = contactsState.toSerializableState()
        val searchSerializableState = databaseState.toSerializableState()

        return if (contactsSerializableState != null && searchSerializableState != null)
            SerializableState(contactsSerializableState, searchSerializableState, query)
        else
            null
    }

    override fun processViewEvent(viewEvent: FindFriendViewEvent): FindFriendState {
        val newQuery = (viewEvent as? FindFriendViewEvent.Search)?.query ?: query

        return FindFriendState(
                contactsState.processViewEvent(viewEvent),
                databaseState.processViewEvent(viewEvent),
                newQuery
        )
    }

    @Parcelize
    data class SerializableState(
            private val contactsState: ContactsState.SerializableState,
            private val databaseState: DatabaseState.SerializableState,
            private val query: String,
    ) : ModelState.SerializableState<FindFriendViewEvent, FindFriendViewModel> {

        override fun toModelState(viewModel: FindFriendViewModel) =
                FindFriendState(contactsState.toModelState(viewModel), databaseState.toModelState(viewModel), query)
    }
}