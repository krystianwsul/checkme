package com.krystianwsul.checkme.gui.friends.findfriend.viewmodel

sealed class FindFriendViewState {

    object Permissions : FindFriendViewState()

    object Loading : FindFriendViewState()

    data class Loaded(
            val people: List<FindFriendViewModel.Person>,
            val showProgress: Boolean,
    ) : FindFriendViewState()
}