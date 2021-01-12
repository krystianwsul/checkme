package com.krystianwsul.checkme.gui.friends.findfriend.viewmodel

import android.os.Parcelable
import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.core.Single

interface ViewModelState<ViewAction : Any, VModel : ViewModel> {

    val nextStateSingle: Single<out ViewModelState<ViewAction, VModel>> // non-empty only for transient

    fun toSerializableState(): SerializableState<ViewAction, VModel>? // null means the state is transient

    fun processViewAction(viewAction: ViewAction): ViewModelState<ViewAction, VModel>

    interface SerializableState<ViewAction : Any, VModel : ViewModel> : Parcelable {

        fun toState(viewModel: VModel): ViewModelState<ViewAction, VModel>
    }
}