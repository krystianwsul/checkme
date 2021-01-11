package com.krystianwsul.checkme.gui.friends.findfriend.viewmodel

import android.os.Parcelable
import androidx.annotation.StringRes
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.firebase.AndroidDatabaseWrapper
import com.krystianwsul.common.firebase.UserData
import com.krystianwsul.common.firebase.json.UserWrapper
import io.reactivex.rxjava3.core.Single
import kotlinx.parcelize.Parcelize

sealed class SearchState : Parcelable, ViewModelState { // todo friend remove parcelable

    abstract val viewState: FindFriendViewModel.ViewState

    open val nextStateSingle: Single<SearchState>? = null

    abstract override fun toSerializableState(): SerializableState?

    open fun processViewAction(viewAction: FindFriendViewModel.ViewAction): SearchState? = null

    @Parcelize
    object Initial : SearchState() { // todo friend merge with Found

        override val viewState get() = FindFriendViewModel.ViewState.Loaded(listOf())

        override fun toSerializableState() = SerializableState.Idle(null)

        override fun processViewAction(viewAction: FindFriendViewModel.ViewAction): SearchState? {
            return when (viewAction) {
                is FindFriendViewModel.ViewAction.Search -> Loading(viewAction.email)
                else -> super.processViewAction(viewAction)
            }
        }
    }

    @Parcelize
    data class Loading(val email: String) : SearchState() {

        init {
            check(email.isNotEmpty())
        }

        override val viewState get() = FindFriendViewModel.ViewState.Loading

        override fun toSerializableState() = SerializableState.Loading(email)

        override val nextStateSingle
            get() = AndroidDatabaseWrapper.getUserObservable(UserData.getKey(email))
                    .firstOrError()
                    .map {
                        if (it.exists())
                            Found(it.getValue(UserWrapper::class.java)!!)
                        else
                            Error(R.string.userNotFound, Initial)
                    }!!
    }

    @Parcelize
    data class Found(val userWrapper: UserWrapper) : SearchState() {

        override val viewState
            get() =
                userWrapper.userData.run { FindFriendViewModel.ViewState.Loaded(listOf(FindFriendViewModel.Contact(name, email, photoUrl, userWrapper))) }

        override fun toSerializableState() = SerializableState.Idle(userWrapper)

        override fun processViewAction(viewAction: FindFriendViewModel.ViewAction): SearchState? {
            return when (viewAction) {
                is FindFriendViewModel.ViewAction.Search -> Loading(viewAction.email)
                else -> super.processViewAction(viewAction)
            }
        }
    }

    @Parcelize
    data class Error(@StringRes private val stringRes: Int, private val nextState: SearchState) : SearchState() {

        override val viewState get() = FindFriendViewModel.ViewState.Error(stringRes)

        override fun toSerializableState(): SerializableState? = null

        override val nextStateSingle get() = Single.just(nextState)!!
    }

    sealed class SerializableState : ViewModelState.SerializableState {

        abstract override fun toState(): SearchState

        @Parcelize
        data class Loading(val email: String) : SerializableState() {

            override fun toState() = SearchState.Loading(email)
        }

        @Parcelize
        data class Idle(val userWrapper: UserWrapper?) : SerializableState() {

            override fun toState() = userWrapper?.let(::Found) ?: Initial
        }
    }
}