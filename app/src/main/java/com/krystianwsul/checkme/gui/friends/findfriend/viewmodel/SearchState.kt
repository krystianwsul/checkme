package com.krystianwsul.checkme.gui.friends.findfriend.viewmodel

import com.krystianwsul.checkme.firebase.AndroidDatabaseWrapper
import com.krystianwsul.common.firebase.json.UserWrapper
import io.reactivex.rxjava3.core.Single
import kotlinx.parcelize.Parcelize

sealed class SearchState : ViewModelState<FindFriendViewModel.ViewAction> {

    override val nextStateSingle: Single<out SearchState> = Single.never()

    abstract override fun toSerializableState(): SerializableState?

    override fun processViewAction(viewAction: FindFriendViewModel.ViewAction): SearchState = this

    object Loading : SearchState() {

        override fun toSerializableState() = SerializableState.Loading

        override val nextStateSingle = AndroidDatabaseWrapper.getUsersObservable()
                .firstOrError() // todo friend make observable
                .map { Loaded(it.children.map { it.getValue(UserWrapper::class.java)!! }) }!!
    }

    data class Loaded(val userWrappers: List<UserWrapper>) : SearchState() {

        override fun toSerializableState() = SerializableState.Loaded(userWrappers)
    }

    sealed class SerializableState : ViewModelState.SerializableState<FindFriendViewModel.ViewAction> {

        abstract override fun toState(): SearchState

        @Parcelize
        object Loading : SerializableState() {

            override fun toState() = SearchState.Loading
        }

        @Parcelize
        data class Loaded(val userWrappers: List<UserWrapper>) : SerializableState() {

            override fun toState() = SearchState.Loaded(userWrappers)
        }
    }
}