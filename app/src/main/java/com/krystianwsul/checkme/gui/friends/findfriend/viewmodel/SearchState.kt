package com.krystianwsul.checkme.gui.friends.findfriend.viewmodel

import android.os.Parcelable
import androidx.annotation.StringRes
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.firebase.AndroidDatabaseWrapper
import com.krystianwsul.common.firebase.UserData
import com.krystianwsul.common.firebase.json.UserWrapper
import io.reactivex.rxjava3.core.Single
import kotlinx.parcelize.Parcelize

sealed class SearchState : Parcelable {

    abstract val viewState: FindFriendViewModel.ViewState

    open val nextStateSingle: Single<SearchState>? = null

    open fun processViewAction(viewAction: FindFriendViewModel.ViewAction): SearchState? = null

    @Parcelize
    object None : SearchState() {

        override val viewState get() = FindFriendViewModel.ViewState.Loaded(listOf())

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

        override val nextStateSingle
            get() = AndroidDatabaseWrapper.getUserObservable(UserData.getKey(email))
                    .firstOrError()
                    .map {
                        if (it.exists())
                            Found(it.getValue(UserWrapper::class.java)!!)
                        else
                            Error(R.string.userNotFound, None)
                    }!!
    }

    @Parcelize
    data class Found(val userWrapper: UserWrapper) : SearchState() {

        override val viewState
            get() =
                userWrapper.userData.run { FindFriendViewModel.ViewState.Loaded(listOf(FindFriendViewModel.Contact(name, email, photoUrl, userWrapper))) }

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

        override val nextStateSingle get() = Single.just(nextState)!!
    }
}