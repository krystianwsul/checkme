package com.krystianwsul.checkme

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.preference.PreferenceManager
import com.akexorcist.localizationactivity.core.LocalizationApplicationDelegate
import com.androidhuman.rxfirebase2.auth.authStateChanges
import com.github.anrwatchdog.ANRWatchDog
import com.github.tamir7.contacts.Contacts
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Logger
import com.google.firebase.messaging.FirebaseMessaging
import com.jakewharton.rxrelay3.BehaviorRelay
import com.krystianwsul.checkme.domainmodel.*
import com.krystianwsul.checkme.domainmodel.extensions.updatePhotoUrl
import com.krystianwsul.checkme.domainmodel.notifications.ImageManager
import com.krystianwsul.checkme.domainmodel.update.AndroidDomainUpdater
import com.krystianwsul.checkme.firebase.AndroidDatabaseWrapper
import com.krystianwsul.checkme.firebase.loaders.FactoryLoader
import com.krystianwsul.checkme.firebase.loaders.FactoryProvider
import com.krystianwsul.checkme.gui.edit.EditActivity
import com.krystianwsul.checkme.gui.main.MainActivity
import com.krystianwsul.checkme.upload.Queue
import com.krystianwsul.checkme.upload.Uploader
import com.krystianwsul.checkme.utils.mapNotNull
import com.krystianwsul.checkme.utils.toSingle
import com.krystianwsul.checkme.utils.toV3
import com.krystianwsul.checkme.viewmodels.NullableWrapper
import com.krystianwsul.common.domain.UserInfo
import com.krystianwsul.common.firebase.DomainThreadChecker
import com.miguelbcr.ui.rx_paparazzo2.RxPaparazzo
import com.mindorks.scheduler.Priority
import com.pacoworks.rxpaper2.RxPaperBook
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.schedulers.Schedulers
import rxdogtag2.RxDogTag
import java.io.File
import java.util.*

class MyApplication : Application() {

    @Suppress("ObjectPropertyName")
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

    private val localizationDelegate = LocalizationApplicationDelegate()

    var defaultLocale: Locale = Locale.getDefault()
        private set

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(localizationDelegate.attachBaseContext(base))
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        defaultLocale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            newConfig.locales.get(0) ?: Locale.getDefault()
        } else {
            @Suppress("DEPRECATION")
            newConfig.locale
        }

        Preferences.language.applySettingStartup()

        super.onConfigurationChanged(newConfig)
        localizationDelegate.onConfigurationChanged(this)
    }

    override fun getApplicationContext(): Context {
        return localizationDelegate.getApplicationContext(super.getApplicationContext())
    }

    override fun getResources(): Resources {
        return localizationDelegate.getResources(baseContext, super.getResources())
    }

    @SuppressLint("CheckResult")
    override fun onCreate() {
        super.onCreate()

        instance = this
        _context = this
        _sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        MyCrashlytics.init()

        RxDogTag.install()

        DomainThreadChecker.instance = AndroidDomainThreadChecker().also {
            runOnDomain(Priority.IMMEDIATE) { it.setDomainThread() }
        }

        AndroidDomainUpdater.init()

        Preferences.language.applySettingStartup()
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

        RxPaperBook.init(this)
        VersionCodeManager.check(AndroidDatabaseWrapper::onUpgrade)

        val factoryLoader = FactoryLoader(userInfoRelay, FactoryProvider.Impl(NoBackup.uuid), Preferences.tokenRelay)

        factoryLoader.userScopeObservable.subscribe(UserScope.instanceRelay)

        factoryLoader.domainFactoryObservable.subscribe {
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
        }
            .mapNotNull { it.value?.photoUrl }
            .flatMapCompletable {
                AndroidDomainUpdater.updatePhotoUrl(DomainListenerManager.NotificationType.All, it.toString())
            }
            .subscribe()

        RxPaparazzo.register(this)

        Queue.init()

        Queue.ready.subscribe { clearPaparazzo() }

        Uploader.resume()

        ImageManager.init()

        Contacts.initialize(this)

        fun createShortcut(
            id: String,
            @StringRes label: Int,
            @DrawableRes icon: Int,
            intent: Intent,
            rank: Int,
        ): ShortcutInfoCompat {
            return ShortcutInfoCompat.Builder(this, id)
                .setShortLabel(getString(label))
                .setIcon(IconCompat.createWithResource(this, icon))
                .setIntent(intent)
                .setRank(rank)
                .build()
        }

        Completable.fromCallable {
            ShortcutManagerCompat.addDynamicShortcuts(
                this,
                listOf(
                    createShortcut(
                        "add",
                        R.string.add_task,
                        R.mipmap.launcher_add,
                        EditActivity.getShortcutIntent(null).addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT),
                        0,
                    ),
                    createShortcut(
                        "search",
                        R.string.search,
                        R.mipmap.launcher_search,
                        MainActivity.newIntent().setAction(MainActivity.ACTION_SEARCH),
                        1,
                    ),
                    createShortcut(
                        "notes",
                        R.string.notes,
                        R.mipmap.launcher_notes,
                        MainActivity.newIntent().setAction(MainActivity.ACTION_NOTES),
                        2,
                    ),
                    createShortcut(
                        "instances",
                        R.string.instances,
                        R.mipmap.launcher_instances,
                        MainActivity.newIntent().setAction(MainActivity.ACTION_INSTANCES),
                        3,
                    ),
                ),
            )
        }
            .subscribeOn(Schedulers.io())
            .subscribe()

        HasInstancesStore.init()
    }

    fun getRxPaparazzoDir() = File(instance.filesDir.absolutePath + "/RxPaparazzo/")

    private fun clearPaparazzo() {
        val queued = Queue.getEntries().map { it.path }

        (getRxPaparazzoDir().listFiles() ?: arrayOf())
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
