package com.krystianwsul.checkme.gui.friends.findfriend.viewmodel

import com.krystianwsul.common.utils.singleOrEmpty
import io.reactivex.rxjava3.core.Single
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

    val nextStateSingle: Single<FindFriendState>
        get() {
            val nextContactsStateSingle = contactsState.nextStateSingle
            val nextSearchStateSingle = searchState.nextStateSingle

            check((nextContactsStateSingle == null) || (nextSearchStateSingle == null))

            return nextContactsStateSingle?.map { FindFriendState(it, searchState) }
                    ?: nextSearchStateSingle?.map { FindFriendState(contactsState, it) }
                    ?: Single.never()
        }

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