package com.krystianwsul.checkme.gui.friends.findfriend.viewmodel

import android.os.Parcelable
import com.krystianwsul.common.utils.singleOrEmpty
import io.reactivex.rxjava3.core.Single
import kotlinx.parcelize.Parcelize

@Parcelize
data class State(val contactsState: ContactsState, val searchState: SearchState) : Parcelable {

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

    val nextStateSingle: Single<State>
        get() {
            val nextContactsStateSingle = contactsState.nextStateSingle
            val nextSearchStateSingle = searchState.nextStateSingle

            check((nextContactsStateSingle == null) || (nextSearchStateSingle == null))

            return nextContactsStateSingle?.map { State(it, searchState) }
                    ?: nextSearchStateSingle?.map { State(contactsState, it) }
                    ?: Single.never()
        }

    fun processViewAction(viewAction: FindFriendViewModel.ViewAction): State {
        val nextContactsState = contactsState.processViewAction(viewAction)
        val nextSearchState = searchState.processViewAction(viewAction)

        return if (nextContactsState != null) {
            check(nextSearchState == null)

            State(nextContactsState, searchState)
        } else {
            checkNotNull(nextSearchState)

            State(contactsState, nextSearchState)
        }
    }
}