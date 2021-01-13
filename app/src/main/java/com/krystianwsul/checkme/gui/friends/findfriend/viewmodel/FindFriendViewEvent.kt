package com.krystianwsul.checkme.gui.friends.findfriend.viewmodel

sealed class FindFriendViewEvent {

    data class Permissions(val granted: Boolean) : FindFriendViewEvent()

    data class Search(val query: String) : FindFriendViewEvent()
}