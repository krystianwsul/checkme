package com.krystianwsul.checkme.gui.friends.findfriend.viewmodel

import com.krystianwsul.common.utils.singleOrEmpty
import io.reactivex.rxjava3.kotlin.merge
import kotlinx.parcelize.Parcelize

data class FindFriendState(val contactsState: ContactsState, val searchState: SearchState) : ViewModelState {

    val viewState: FindFriendViewModel.ViewState
        get() {
            val contactsViewState = contactsState.viewState
            val searchViewState = searchState.viewState
            val viewStates = listOf(searchViewState, contactsViewState)

            viewStates.filterIsInstance<FindFriendViewModel.ViewState.Error>()
                    .singleOrEmpty()
                    ?.let { return it }

            viewStates.filterIsInstance<FindFriendViewModel.ViewState.Permissions>()
                    .singleOrEmpty()
                    ?.let { return it }

            viewStates.filterIsInstance<FindFriendViewModel.ViewState.Loading>()
                    .firstOrNull()
                    ?.let { return it }

            return viewStates.map { it as FindFriendViewModel.ViewState.Loaded }.let {
                FindFriendViewModel.ViewState.Loaded(
                        it.map { it.contacts }
                                .flatten()
                                .distinctBy { it.email }
                )
            }
        }

    override val nextStateSingle = listOf(
            contactsState.nextStateSingle.map { FindFriendState(it, searchState) },
            searchState.nextStateSingle.map { FindFriendState(contactsState, it) },
    ).map { it.toObservable() }
            .merge()
            .firstOrError()!!

    override fun toSerializableState() = searchState.toSerializableState()?.let {
        SerializableState(contactsState.toSerializableState(), it)
    }

    fun processViewAction(viewAction: FindFriendViewModel.ViewAction): FindFriendState {
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