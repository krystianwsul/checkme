package com.krystianwsul.checkme.gui.friends.findfriend.viewmodel

import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.jakewharton.rxrelay3.BehaviorRelay
import com.jakewharton.rxrelay3.PublishRelay
import com.krystianwsul.checkme.utils.mapNotNull
import com.krystianwsul.common.firebase.json.UserWrapper
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import kotlinx.parcelize.Parcelize

class FindFriendViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {

    companion object {

        private const val KEY_STATE = "state"
    }

    private val clearedDisposable = CompositeDisposable()

    private val stateRelay = BehaviorRelay.createDefault(
            savedStateHandle.get<FindFriendState.SerializableState>(KEY_STATE)
                    ?.toState()
                    ?: FindFriendState(ContactsState.Initial, SearchState.Idle(null))
    )

    private val viewStateRelay = RxQueue<ViewState>()

    init {
        stateRelay.map { it.getViewState() }
                .distinctUntilChanged()
                .subscribe(viewStateRelay::accept)
                .addTo(clearedDisposable)
    }

    val viewStateObservable = viewStateRelay.distinctUntilChanged()!!

    val viewActionRelay = PublishRelay.create<ViewAction>()!!

    init {
        stateRelay.mapNotNull { it.toSerializableState() }
                .subscribe { savedStateHandle[KEY_STATE] = it }
                .addTo(clearedDisposable)

        stateRelay.switchMapSingle { it.nextStateSingle }
                .subscribe(stateRelay::accept)
                .addTo(clearedDisposable)

        stateRelay.switchMapSingle { viewActionRelay.firstOrError().map { viewAction -> it to viewAction } }
                .map { (state, viewAction) -> state.processViewAction(viewAction) }
                .subscribe(stateRelay::accept)
                .addTo(clearedDisposable)
    }

    override fun onCleared() = clearedDisposable.dispose()

    @Parcelize
    data class Contact(
            val displayName: String,
            val email: String,
            val photoUri: String?,
            val userWrapper: UserWrapper?,
    ) : Parcelable

    sealed class ViewState {

        data class Error(@StringRes val stringRes: Int) : ViewState()

        object Permissions : ViewState()

        object Loading : ViewState()

        data class Loaded(val contacts: List<Contact>, val showProgress: Boolean) : ViewState()
    }

    sealed class ViewAction {

        data class Permissions(val granted: Boolean) : ViewAction()

        data class Search(val email: String) : ViewAction()
    }
}