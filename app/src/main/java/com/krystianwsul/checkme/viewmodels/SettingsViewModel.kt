package com.krystianwsul.checkme.viewmodels

import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.jakewharton.rxrelay2.PublishRelay
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.utils.toSingle
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo

class SettingsViewModel : DomainViewModel<SettingsViewModel.Data>() {

    override val domainListener = object : DomainListener<Data>() {

        override fun getData(domainFactory: DomainFactory) = domainFactory.getSettingsData()
    }

    fun start() = internalStart()

    private val compositeDisposable = CompositeDisposable()

    val relay = PublishRelay.create<NullableWrapper<GoogleSignInAccount>>()

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

    data class Data(val defaultReminder: Boolean) : DomainData()
}