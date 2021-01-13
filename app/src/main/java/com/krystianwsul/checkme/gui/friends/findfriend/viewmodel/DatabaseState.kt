package com.krystianwsul.checkme.gui.friends.findfriend.viewmodel

import com.krystianwsul.checkme.gui.friends.findfriend.viewmodel.DatabaseState.Loaded
import com.krystianwsul.checkme.gui.friends.findfriend.viewmodel.DatabaseState.Loading
import com.krystianwsul.common.firebase.json.UserWrapper
import kotlinx.parcelize.Parcelize

sealed class DatabaseState(viewModel: FindFriendViewModel) :
        ModelState<FindFriendViewEvent, FindFriendViewModel> {

    override val nextStateSingle = viewModel.usersObservable
            .firstOrError()
            .map { Loaded(viewModel, it.children.map { it.getValue(UserWrapper::class.java)!! }) }!!

    abstract override fun toSerializableState(): SerializableState?

    override fun processViewEvent(viewEvent: FindFriendViewEvent): DatabaseState = this

    class Loading(viewModel: FindFriendViewModel) : DatabaseState(viewModel) {

        override fun toSerializableState() = SerializableState.Loading
    }

    class Loaded(
            viewModel: FindFriendViewModel,
            val userWrappers: List<UserWrapper>,
    ) : DatabaseState(viewModel) {

        override fun toSerializableState() = SerializableState.Loaded(userWrappers)
    }

    sealed class SerializableState :
            ModelState.SerializableState<FindFriendViewEvent, FindFriendViewModel> {

        abstract override fun toModelState(viewModel: FindFriendViewModel): DatabaseState

        @Parcelize
        object Loading : SerializableState() {

            override fun toModelState(viewModel: FindFriendViewModel) = Loading(viewModel)
        }

        @Parcelize
        data class Loaded(val userWrappers: List<UserWrapper>) : SerializableState() {

            override fun toModelState(viewModel: FindFriendViewModel) = Loaded(viewModel, userWrappers)
        }
    }
}