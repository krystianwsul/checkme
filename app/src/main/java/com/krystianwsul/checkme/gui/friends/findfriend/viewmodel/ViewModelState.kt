package com.krystianwsul.checkme.gui.friends.findfriend.viewmodel

import android.os.Parcelable
import io.reactivex.rxjava3.core.Single

interface ViewModelState<ViewAction : Any> {

    val nextStateSingle: Single<out ViewModelState<ViewAction>> // non-empty only for transient

    fun toSerializableState(): SerializableState<ViewAction>? // null means the state is transient

    fun processViewAction(viewAction: ViewAction): ViewModelState<ViewAction>

    interface SerializableState<ViewAction : Any> : Parcelable {

        fun toState(): ViewModelState<ViewAction>
    }
}