package com.krystianwsul.checkme

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.androidhuman.rxfirebase2.auth.authStateChanges
import com.github.anrwatchdog.ANRWatchDog
import com.github.tamir7.contacts.Contacts
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Logger
import com.google.firebase.messaging.FirebaseMessaging
import com.jakewharton.rxrelay3.BehaviorRelay
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.extensions.updatePhotoUrl
import com.krystianwsul.checkme.domainmodel.local.LocalFactory
import com.krystianwsul.checkme.domainmodel.notifications.ImageManager
import com.krystianwsul.checkme.domainmodel.toUserInfo
import com.krystianwsul.checkme.firebase.loaders.FactoryLoader
import com.krystianwsul.checkme.firebase.loaders.FactoryProvider
import com.krystianwsul.checkme.persistencemodel.PersistenceManager
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.upload.Queue
import com.krystianwsul.checkme.upload.Uploader
import com.krystianwsul.checkme.utils.toSingle
import com.krystianwsul.checkme.utils.toV3
import com.krystianwsul.checkme.viewmodels.NullableWrapper
import com.krystianwsul.common.domain.UserInfo
import com.miguelbcr.ui.rx_paparazzo2.RxPaparazzo
import com.pacoworks.rxpaper2.RxPaperBook
import io.reactivex.rxjava3.core.Maybe
import net.danlew.android.joda.JodaTimeAndroid
import rxdogtag2.RxDogTag
import java.io.File

class MyApplication : Application() {

    companion object {

        @SuppressLint("StaticFieldLeak")
        lateinit var instance: MyApplication
            private set

        @SuppressLint("StaticFieldLeak")
        var _context: Context? = null
        val context get() = _context!!

        var _sharedPreferences: SharedPreferences? = null
        val sharedPreferences get() = _sharedPreferences!!
    }

    val googleSignInClient by lazy { getClient() }

    private val userInfoRelay = BehaviorRelay.createDefault(NullableWrapper<UserInfo>())

    val hasUserInfo get() = userInfoRelay.value!!.value != null
    val userInfo get() = userInfoRelay.value!!.value!!

    @SuppressLint("CheckResult")
    override fun onCreate() {
        super.onCreate()

        instance = this
        _context = this
        _sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        MyCrashlytics.init()

        RxDogTag.install()

        JodaTimeAndroid.init(this)

        Preferences.tickLog.logLineDate("MyApplication.onCreate")

        FirebaseDatabase.getInstance().apply {
            setLogLevel(Logger.Level.DEBUG)
            setPersistenceEnabled(true)
        }

        FirebaseAuth.getInstance()
                .authStateChanges()
                .toV3()
                .map { NullableWrapper(it.currentUser) }
                .startWithItem(NullableWrapper(FirebaseAuth.getInstance().currentUser))
                .map { NullableWrapper(it.value?.toUserInfo()) }
                .distinctUntilChanged()
                .subscribe { userInfoRelay.accept(it) }

        userInfoRelay.firstOrError()
                .filter { it.value != null }
                .subscribe { DomainFactory.firstRun = true }

        val localFactory = LocalFactory(PersistenceManager.instance)

        FactoryLoader(
                localFactory,
                userInfoRelay,
                FactoryProvider.Impl(localFactory),
                Preferences.tokenRelay
        ).domainFactoryObservable.subscribe {
            @Suppress("UNCHECKED_CAST")
            DomainFactory.instanceRelay.accept(it as NullableWrapper<DomainFactory>)
        }

        if (Preferences.token == null)
            FirebaseMessaging.getInstance()
                    .token
                    .addOnSuccessListener { Preferences.token = it }

        //writeHashes()

        if (!BuildConfig.DEBUG)
            ANRWatchDog()//.setReportMainThreadOnly()
                    .setANRListener { MyCrashlytics.logException(it) }
                    .start()

        userInfoRelay.switchMapMaybe {
            it.value
                    ?.let {
                        googleSignInClient.silentSignIn()
                                .toSingle()
                                .toMaybe()
                    }
                    ?: Maybe.empty()
        }.subscribe {
            it.value
                    ?.photoUrl
                    ?.let { url ->
                        DomainFactory.addFirebaseListener { it.updatePhotoUrl(SaveService.Source.GUI, url.toString()) }
                    }
        }

        RxPaparazzo.register(this)

        RxPaperBook.init(this)

        Queue.init()

        Queue.ready.subscribe { clearPaparazzo() }

        Uploader.resume()

        ImageManager.init()

        Contacts.initialize(this)
    }

    private fun clearPaparazzo() {
        val paparazzo = instance.filesDir.absolutePath + "/RxPaparazzo/"

        val queued = Queue.getEntries().map { it.path }

        (File(paparazzo).listFiles() ?: arrayOf())
                .filterNot { it.absolutePath in queued }
                .forEach {
                    try {
                        it.delete()
                    } catch (e: Exception) {
                        MyCrashlytics.logException(e)
                    }
                }
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
