package com.krystianwsul.checkme.gui.friends.findfriend.viewmodel

import android.os.Parcelable
import io.reactivex.rxjava3.core.Single

interface ViewModelState {

    val nextStateSingle: Single<out ViewModelState>

    fun toSerializableState(): SerializableState? // null means the state is transient

    interface SerializableState : Parcelable {

        fun toState(): ViewModelState
    }
}