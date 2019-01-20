package com.krystianwsul.checkme

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.preference.PreferenceManager
import android.util.Base64
import android.util.Log
import com.androidhuman.rxfirebase2.auth.authStateChanges
import com.github.anrwatchdog.ANRWatchDog
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Logger
import com.google.firebase.iid.FirebaseInstanceId
import com.jakewharton.rxrelay2.BehaviorRelay
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.UserInfo
import com.krystianwsul.checkme.firebase.DatabaseWrapper
import com.krystianwsul.checkme.persistencemodel.PersistenceManager
import com.krystianwsul.checkme.viewmodels.NullableWrapper
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.rxkotlin.Observables
import net.danlew.android.joda.JodaTimeAndroid
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

class MyApplication : Application() {

    companion object {

        private const val TOKEN_KEY = "token"

        @SuppressLint("StaticFieldLeak")
        lateinit var instance: MyApplication
            private set

        // for unit tests
        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context

        lateinit var sharedPreferences: SharedPreferences
    }

    var token: String?
        get() = sharedPreferences.getString(TOKEN_KEY, null)
        set(value) = sharedPreferences.edit()
                .putString(TOKEN_KEY, value)
                .apply()

    val googleSigninClient by lazy {
        GoogleSignIn.getClient(this, GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build())
    }

    val userInfoRelay = BehaviorRelay.createDefault(NullableWrapper<UserInfo>())

    val userInfo get() = userInfoRelay.value!!.value!!

    val hasUserInfo get() = userInfoRelay.value!!.value != null

    @SuppressLint("CheckResult")
    override fun onCreate() {
        super.onCreate()

        instance = this
        context = this
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        JodaTimeAndroid.init(this)

        Preferences.logLineDate("MyApplication.onCreate")

        FirebaseDatabase.getInstance().apply {
            setLogLevel(Logger.Level.DEBUG)
            setPersistenceEnabled(true)
        }

        FirebaseAuth.getInstance()
                .authStateChanges()
                .map { NullableWrapper(it.currentUser) }
                .startWith(NullableWrapper(FirebaseAuth.getInstance().currentUser))
                .map { NullableWrapper(it.value?.let { UserInfo(it) }) }
                .subscribe(userInfoRelay)

        userInfoRelay.switchMapSingle {
            if (it.value != null) {
                check(DomainFactory.nullableInstance == null)
                Observables.combineLatest(DatabaseWrapper.tasks, DatabaseWrapper.user, DatabaseWrapper.friends) { tasks: DataSnapshot, user: DataSnapshot, friends: DataSnapshot -> Pair(it.value, Triple(tasks, user, friends)) }.firstOrError()
            } else {
                DomainFactory.nullableInstance?.clearUserInfo()
                DomainFactory.instanceRelay.accept(NullableWrapper())

                Single.never()
            }
        }.subscribe {
            val userInfo = it.first
            val (tasks, user, friends) = it.second

            DomainFactory.instanceRelay.accept(NullableWrapper(DomainFactory(PersistenceManager.instance, userInfo).apply {
                setRemoteTaskRecords(tasks)
                setUserRecord(user)
                setFriendRecords(friends)
            }))
        }

        fun ifDomain(observable: Observable<DataSnapshot>, setter: DomainFactory.(DataSnapshot) -> Unit) = DomainFactory.instanceRelay
                .switchMap { if (it.value != null) observable.map { dataSnapshot -> Pair(it.value, dataSnapshot) } else Observable.never() }
                .skip(1)
                .subscribe { it.first.setter(it.second) }

        ifDomain(DatabaseWrapper.tasks, DomainFactory::setRemoteTaskRecords)
        ifDomain(DatabaseWrapper.user, DomainFactory::setUserRecord)
        ifDomain(DatabaseWrapper.friends, DomainFactory::setFriendRecords)

        if (token == null)
            FirebaseInstanceId.getInstance()
                    .instanceId
                    .addOnSuccessListener { token = it.token }

        writeHashes()

        ANRWatchDog()//.setReportMainThreadOnly()
                .setANRListener { MyCrashlytics.logException(it) }
                .start()
    }

    @Suppress("DEPRECATION")
    @SuppressLint("PackageManagerGetSignatures")
    private fun writeHashes() {
        try {
            Log.e("asdf", "getting hash for $packageName")
            val info = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            for (signature in info.signatures) {
                val md = MessageDigest.getInstance("SHA")
                md.update(signature.toByteArray())

                val digest = md.digest()

                fun byteArrayToHex(a: ByteArray): String {
                    val sb = StringBuilder(a.size * 2)
                    for (b in a)
                        sb.append(String.format("%02x", b))
                    return sb.toString()
                }

                Log.e("asdf", "google hash: " + byteArrayToHex(digest).toUpperCase().chunked(2).joinToString(":"))
                Log.e("asdf", "facebook hash: " + Base64.encodeToString(digest, Base64.DEFAULT))
            }
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e("asdf", "hash error", e)
        } catch (e: NoSuchAlgorithmException) {
            Log.e("asdf", "hash error", e)
        }
    }
}
