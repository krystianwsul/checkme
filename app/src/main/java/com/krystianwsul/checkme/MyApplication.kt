package com.krystianwsul.checkme

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import com.androidhuman.rxfirebase2.auth.authStateChanges
import com.github.anrwatchdog.ANRWatchDog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Logger
import com.google.firebase.iid.FirebaseInstanceId
import com.jakewharton.rxrelay2.BehaviorRelay
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.UserInfo
import com.krystianwsul.checkme.firebase.DatabaseWrapper
import com.krystianwsul.checkme.firebase.FactoryListener
import com.krystianwsul.checkme.persistencemodel.PersistenceManager
import com.krystianwsul.checkme.utils.time.ExactTimeStamp
import com.krystianwsul.checkme.viewmodels.NullableWrapper
import io.reactivex.Observable
import net.danlew.android.joda.JodaTimeAndroid

class MyApplication : Application() {

    companion object {

        private const val TOKEN_KEY = "token"
        private const val DEFAULT_REMOTE_KEY = "defaultRemote"

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

    val googleSigninClient by lazy { getClient() }

    private val userInfoRelay = BehaviorRelay.createDefault(NullableWrapper<UserInfo>())

    val userInfo get() = userInfoRelay.value!!.value!!

    val hasUserInfo get() = userInfoRelay.value!!.value != null

    var defaultRemote: Boolean
        get() = sharedPreferences.getBoolean(DEFAULT_REMOTE_KEY, false)
        set(value) {
            sharedPreferences.edit()
                    .putBoolean(DEFAULT_REMOTE_KEY, value)
                    .apply()
        }

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
                .distinctUntilChanged()
                .subscribe(userInfoRelay)

        userInfoRelay.switchMap {
            MyCrashlytics.log("userInfoRelay: $it")
            it.value?.let { DatabaseWrapper.getPrivateProjectObservable(it.key) }
                    ?: Observable.never()
        }
                .subscribe { MyCrashlytics.log("independent private project event") }

        FactoryListener(
                userInfoRelay,
                { DatabaseWrapper.getPrivateProjectSingle(it.key) },
                { DatabaseWrapper.getSharedProjectSingle(it.key) },
                { DatabaseWrapper.getFriendSingle(it.key) },
                { DatabaseWrapper.getUserSingle(it.key) },
                { DatabaseWrapper.getPrivateProjectObservable(it.key) },
                { DatabaseWrapper.getSharedProjectEvents(it.key) },
                { DatabaseWrapper.getFriendObservable(it.key) },
                { DatabaseWrapper.getUserObservable(it.key) },
                { userInfo, privateProject, sharedProjects, friends, user -> DomainFactory(PersistenceManager.instance, userInfo, ExactTimeStamp.now, sharedProjects, privateProject, user, friends) },
                { DomainFactory.nullableInstance?.clearUserInfo() },
                DomainFactory::updatePrivateProjectRecord,
                DomainFactory::updateSharedProjectRecords,
                DomainFactory::setFriendRecords,
                DomainFactory::setUserRecord
                //, { Log.e("asdf", "FactoryListener:\n$it")}
        ).domainFactoryObservable.subscribe(DomainFactory.instanceRelay)

        if (token == null)
            FirebaseInstanceId.getInstance()
                    .instanceId
                    .addOnSuccessListener { token = it.token }

        //writeHashes()

        if (!BuildConfig.DEBUG)
            ANRWatchDog()//.setReportMainThreadOnly()
                    .setANRListener { MyCrashlytics.logException(it) }
                    .start()
    }

    /*
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
    */
}
