package com.krystianwsul.checkme

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.androidhuman.rxfirebase2.auth.authStateChanges
import com.github.anrwatchdog.ANRWatchDog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Logger
import com.google.firebase.iid.FirebaseInstanceId
import com.jakewharton.rxrelay2.BehaviorRelay
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.local.LocalFactory
import com.krystianwsul.checkme.domainmodel.notifications.ImageManager
import com.krystianwsul.checkme.domainmodel.toUserInfo
import com.krystianwsul.checkme.firebase.AndroidDatabaseWrapper
import com.krystianwsul.checkme.firebase.FactoryListener
import com.krystianwsul.checkme.firebase.RemoteProjectFactory
import com.krystianwsul.checkme.firebase.RemoteUserFactory
import com.krystianwsul.checkme.persistencemodel.PersistenceManager
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.upload.Queue
import com.krystianwsul.checkme.upload.Uploader
import com.krystianwsul.checkme.utils.toSingle
import com.krystianwsul.checkme.utils.zipSingle
import com.krystianwsul.checkme.viewmodels.NullableWrapper
import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.domain.DeviceInfo
import com.krystianwsul.common.time.ExactTimeStamp
import com.miguelbcr.ui.rx_paparazzo2.RxPaparazzo
import com.pacoworks.rxpaper2.RxPaperBook
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.rxkotlin.merge
import net.danlew.android.joda.JodaTimeAndroid
import java.io.File

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

    val googleSignInClient by lazy { getClient() }

    private val deviceInfoRelay = BehaviorRelay.createDefault(NullableWrapper<DeviceInfo>())

    val hasUserInfo get() = deviceInfoRelay.value!!.value != null
    val userInfo get() = deviceInfoRelay.value!!.value!!

    @SuppressLint("CheckResult")
    override fun onCreate() {
        super.onCreate()

        instance = this
        context = this
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        JodaTimeAndroid.init(this)

        Preferences.tickLog.logLineDate("MyApplication.onCreate")

        FirebaseDatabase.getInstance().apply {
            setLogLevel(Logger.Level.DEBUG)
            setPersistenceEnabled(true)
        }

        FirebaseAuth.getInstance()
                .authStateChanges()
                .map { NullableWrapper(it.currentUser) }
                .startWith(NullableWrapper(FirebaseAuth.getInstance().currentUser))
                .map { NullableWrapper(it.value?.let { DeviceInfo(it.toUserInfo(), token) }) }
                .distinctUntilChanged()
                .subscribe(deviceInfoRelay)

        deviceInfoRelay.firstOrError()
                .filter { it.value != null }
                .subscribe { DomainFactory.firstRun = true }

        val localFactory = LocalFactory(PersistenceManager.instance)

        FactoryListener(
                deviceInfoRelay,
                { AndroidDatabaseWrapper.getUserSingle(it.key) },
                { AndroidDatabaseWrapper.getUserObservable(it.key) },
                { deviceInfo, user -> RemoteUserFactory(localFactory.uuid, user, deviceInfo) },
                { AndroidDatabaseWrapper.getPrivateProjectSingle(it.key.toPrivateProjectKey()) },
                { projectIds -> projectIds.map { AndroidDatabaseWrapper.getSharedProjectSingle(it) }.zipSingle() },
                { AndroidDatabaseWrapper.getPrivateProjectObservable(it.key.toPrivateProjectKey()) },
                { (oldProjectIds, newProjectIds) ->
                    val removedIds = oldProjectIds - newProjectIds

                    val removedEvents = Observable.fromIterable(removedIds.map { RemoteProjectFactory.Event.Remove(it) })

                    val addChangeEvents = newProjectIds.map {
                        AndroidDatabaseWrapper.getSharedProjectObservable(it).map { RemoteProjectFactory.Event.AddChange(it) }
                    }.merge()

                    listOf(removedEvents, addChangeEvents).merge()
                },
                { deviceInfo, privateProject, sharedProjects ->
                    val deviceDbInfo = DeviceDbInfo(deviceInfo, localFactory.uuid)

                    RemoteProjectFactory(
                            deviceDbInfo,
                            localFactory,
                            sharedProjects,
                            privateProject,
                            ExactTimeStamp.now
                    )
                },
                { AndroidDatabaseWrapper.getFriendSingle(it.key) },
                { AndroidDatabaseWrapper.getFriendObservable(it.key) },
                { startTime, deviceInfo, remoteUserFactory, remoteProjectFactory, friends ->
                    val readTime = ExactTimeStamp.now

                    val deviceDbInfo = DeviceDbInfo(deviceInfo, localFactory.uuid)

                    DomainFactory(
                            localFactory,
                            remoteUserFactory,
                            remoteProjectFactory,
                            deviceDbInfo,
                            startTime,
                            readTime,
                            friends
                    )
                },
                { DomainFactory.nullableInstance?.clearUserInfo() },
                DomainFactory::updatePrivateProjectRecord,
                DomainFactory::updateSharedProjectRecords,
                DomainFactory::setFriendRecords,
                DomainFactory::updateUserRecord
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

        deviceInfoRelay.switchMapMaybe {
            it.value?.let {
                googleSignInClient.silentSignIn()
                        .toSingle()
                        .toMaybe()
            } ?: Maybe.empty()
        }.subscribe {
            it.value
                    ?.photoUrl
                    ?.let { url ->
                        DomainFactory.addFirebaseListener {
                            it.updatePhotoUrl(SaveService.Source.GUI, url.toString())
                        }
                    }
        }

        RxPaparazzo.register(this)

        RxPaperBook.init(this)

        Queue.init()

        Queue.ready.subscribe { clearPaparazzo() }

        Uploader.resume()

        ImageManager.init()
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
