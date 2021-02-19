package com.krystianwsul.checkme.viewmodels

import android.content.Intent
import androidx.lifecycle.ViewModel
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.jakewharton.rxrelay3.BehaviorRelay
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.MyCrashlytics

class TutorialViewModel : ViewModel() {

    val state = BehaviorRelay.createDefault<State>(State.Initial)

    fun startSignIn() = state.accept(State.Progress)

    fun onActivityResult(data: Intent?) {
        val googleSignInResult = Auth.GoogleSignInApi.getSignInResultFromIntent(data)!!

        if (googleSignInResult.isSuccess) {
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

                            MyApplication.instance.googleSignInClient.signOut()

                            state.apply {
                                accept(State.Error)
                                accept(State.Initial)
                            }
                        }
                    }
        } else {
            state.apply {
                accept(State.Error)
                accept(State.Initial)
            }

            if (googleSignInResult.status.statusCode != GoogleSignInStatusCodes.CANCELED) {
                MyCrashlytics.logException(GoogleSignInException(
                        googleSignInResult.run { "isSuccess: $isSuccess, status: $status" }
                ))
            }
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