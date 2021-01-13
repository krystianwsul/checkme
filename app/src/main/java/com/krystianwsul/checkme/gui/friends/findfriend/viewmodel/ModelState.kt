package com.krystianwsul.checkme.gui.friends.findfriend.viewmodel

import android.os.Parcelable
import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.core.Single

interface ModelState<ViewEvent : Any, VModel : ViewModel> {

    val nextStateSingle: Single<out ModelState<ViewEvent, VModel>> // non-empty only for transient states

    fun toSerializableState(): SerializableState<ViewEvent, VModel>? // null means the state is transient

    fun processViewEvent(viewEvent: ViewEvent): ModelState<ViewEvent, VModel>

    interface SerializableState<ViewAction : Any, VModel : ViewModel> : Parcelable {

        fun toModelState(viewModel: VModel): ModelState<ViewAction, VModel>
    }
}