package com.krystianwsul.checkme.viewmodels

import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import com.crashlytics.android.answers.CustomEvent
import com.google.android.gms.auth.api.Auth
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.jakewharton.rxrelay2.BehaviorRelay
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.MyCrashlytics

class TutorialViewModel : ViewModel() {

    val state = BehaviorRelay.createDefault<State>(State.Initial)

    fun startSignIn() = state.accept(State.Progress)

    fun onActivityResult(data: Intent?) {
        val googleSignInResult = Auth.GoogleSignInApi.getSignInResultFromIntent(data)!!

        if (googleSignInResult.isSuccess) {
            MyCrashlytics.answers?.logCustom(CustomEvent("google success"))
            val googleSignInAccount = googleSignInResult.signInAccount!!

            val credential = GoogleAuthProvider.getCredential(googleSignInAccount.idToken, null)

            FirebaseAuth.getInstance()
                    .signInWithCredential(credential)
                    .addOnCompleteListener {
                        if (it.isSuccessful) {
                            state.accept(State.Success(it.result!!.user!!.displayName!!))
                        } else {
                            val exception = it.exception!!

                            MyCrashlytics.logException(exception)

                            MyApplication.instance.googleSigninClient.signOut()

                            state.apply {
                                accept(State.Error)
                                accept(State.Initial)
                            }
                        }
                    }
        } else {
            MyCrashlytics.answers?.logCustom(CustomEvent("google error"))
            val message = "google signin error: $googleSignInResult"

            Log.e("asdf", message)

            state.apply {
                accept(State.Error)
                accept(State.Initial)
            }

            MyCrashlytics.logException(GoogleSignInException("isSuccess: " + googleSignInResult.isSuccess + ", status: " + googleSignInResult.status))
        }
    }

    sealed class State {

        object Initial : State()

        object Progress : State()

        object Error : State()

        class Success(val displayName: String) : State()
    }

    private class GoogleSignInException(message: String) : Exception(message)
}