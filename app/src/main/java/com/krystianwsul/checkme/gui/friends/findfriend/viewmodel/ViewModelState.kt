package com.krystianwsul.checkme.gui.friends.findfriend.viewmodel

import android.os.Parcelable

interface ViewModelState {

    fun toSerializableState(): SerializableState? // null means the state is transient

    interface SerializableState : Parcelable {

        fun toState(): ViewModelState
    }
}