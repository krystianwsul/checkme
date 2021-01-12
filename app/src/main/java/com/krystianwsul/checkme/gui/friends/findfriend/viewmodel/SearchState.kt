package com.krystianwsul.checkme.gui.friends.findfriend.viewmodel

import androidx.annotation.StringRes
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.firebase.AndroidDatabaseWrapper
import com.krystianwsul.common.firebase.json.UserWrapper
import io.reactivex.rxjava3.core.Single
import kotlinx.parcelize.Parcelize

sealed class SearchState : ViewModelState<FindFriendViewModel.ViewAction> {

    override val nextStateSingle: Single<SearchState> = Single.never()

    abstract override fun toSerializableState(): SerializableState?

    override fun processViewAction(viewAction: FindFriendViewModel.ViewAction): SearchState = this

    data class Loading(val email: String) : SearchState() {

        init {
            check(email.isNotEmpty())
        }

        override fun toSerializableState() = SerializableState.Loading(email)

        override val nextStateSingle = AndroidDatabaseWrapper.searchUserObservable(email)
                .firstOrError()
                .map {
                    if (it.exists())
                        Idle(it.children.map { it.getValue(UserWrapper::class.java)!! })
                    else
                        Error(R.string.userNotFound, Idle(listOf()))
                }!!
    }

    data class Idle(val userWrappers: List<UserWrapper>) : SearchState() {

        override fun toSerializableState() = SerializableState.Idle(userWrappers)

        override fun processViewAction(viewAction: FindFriendViewModel.ViewAction): SearchState {
            return when (viewAction) {
                is FindFriendViewModel.ViewAction.Search -> Loading(viewAction.email)
                else -> super.processViewAction(viewAction)
            }
        }
    }

    data class Error(@StringRes val stringRes: Int, private val nextState: SearchState) : SearchState() {

        override fun toSerializableState(): SerializableState? = null

        override val nextStateSingle = Single.just(nextState)!!
    }

    sealed class SerializableState : ViewModelState.SerializableState<FindFriendViewModel.ViewAction> {

        abstract override fun toState(): SearchState

        @Parcelize
        data class Loading(val email: String) : SerializableState() {

            override fun toState() = SearchState.Loading(email)
        }

        @Parcelize
        data class Idle(val userWrappers: List<UserWrapper>) : SerializableState() {

            override fun toState() = SearchState.Idle(userWrappers)
        }
    }
}