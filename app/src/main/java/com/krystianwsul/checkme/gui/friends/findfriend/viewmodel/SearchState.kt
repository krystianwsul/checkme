package com.krystianwsul.checkme.gui.friends.findfriend.viewmodel

import com.krystianwsul.checkme.gui.friends.findfriend.viewmodel.SearchState.Loaded
import com.krystianwsul.checkme.gui.friends.findfriend.viewmodel.SearchState.Loading
import com.krystianwsul.common.firebase.json.UserWrapper
import kotlinx.parcelize.Parcelize

sealed class SearchState(viewModel: FindFriendViewModel) :
        ViewModelState<FindFriendViewModel.ViewEvent, FindFriendViewModel> {

    override val nextStateSingle = viewModel.usersObservable
            .firstOrError()
            .map { Loaded(viewModel, it.children.map { it.getValue(UserWrapper::class.java)!! }) }!!

    abstract override fun toSerializableState(): SerializableState?

    override fun processViewAction(viewEvent: FindFriendViewModel.ViewEvent): SearchState = this

    class Loading(viewModel: FindFriendViewModel) : SearchState(viewModel) {

        override fun toSerializableState() = SerializableState.Loading
    }

    class Loaded(
            viewModel: FindFriendViewModel,
            val userWrappers: List<UserWrapper>,
    ) : SearchState(viewModel) {

        override fun toSerializableState() = SerializableState.Loaded(userWrappers)
    }

    sealed class SerializableState :
            ViewModelState.SerializableState<FindFriendViewModel.ViewEvent, FindFriendViewModel> {


        abstract override fun toState(viewModel: FindFriendViewModel): SearchState

        @Parcelize
        object Loading : SerializableState() {

            override fun toState(viewModel: FindFriendViewModel) = Loading(viewModel)
        }

        @Parcelize
        data class Loaded(val userWrappers: List<UserWrapper>) : SerializableState() {

            override fun toState(viewModel: FindFriendViewModel) = Loaded(viewModel, userWrappers)
        }
    }
}