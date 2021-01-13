package com.krystianwsul.checkme.gui.friends.findfriend.viewmodel

import android.os.Parcelable
import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.core.Single

interface ViewModelState<ViewEvent : Any, VModel : ViewModel> {

    val nextStateSingle: Single<out ViewModelState<ViewEvent, VModel>> // non-empty only for transient

    fun toSerializableState(): SerializableState<ViewEvent, VModel>? // null means the state is transient

    fun processViewEvent(viewEvent: ViewEvent): ViewModelState<ViewEvent, VModel>

    interface SerializableState<ViewAction : Any, VModel : ViewModel> : Parcelable {

        fun toState(viewModel: VModel): ViewModelState<ViewAction, VModel>
    }
}