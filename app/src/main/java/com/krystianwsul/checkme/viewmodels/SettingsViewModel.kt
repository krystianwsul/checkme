package com.krystianwsul.checkme.viewmodels

import androidx.lifecycle.ViewModel
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.jakewharton.rxrelay3.PublishRelay
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.utils.toSingle
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo

class SettingsViewModel : ViewModel() {

    private val compositeDisposable = CompositeDisposable()

    val relay = PublishRelay.create<NullableWrapper<GoogleSignInAccount>>()!!

    fun silentSignIn() {
        MyApplication.instance
                .googleSignInClient
                .silentSignIn()
                .toSingle()
                .subscribe(relay)
                .addTo(compositeDisposable)
    }

    override fun onCleared() {
        compositeDisposable.dispose()

        super.onCleared()
    }
}