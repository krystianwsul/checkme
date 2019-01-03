package com.krystianwsul.checkme

import android.annotation.SuppressLint
import android.app.Application
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.preference.PreferenceManager
import android.util.Base64
import android.util.Log
import com.github.anrwatchdog.ANRWatchDog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Logger
import com.google.firebase.iid.FirebaseInstanceId
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.UserInfo
import com.krystianwsul.checkme.persistencemodel.SaveService
import net.danlew.android.joda.JodaTimeAndroid
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

class MyApplication : Application() {

    companion object {

        private const val TOKEN_KEY = "token"

        @SuppressLint("StaticFieldLeak")
        lateinit var instance: MyApplication
            private set
    }

    lateinit var sharedPreferences: SharedPreferences
        private set

    var token: String?
        get() = sharedPreferences.getString(TOKEN_KEY, null)
        set(value) = sharedPreferences.edit()
                .putString(TOKEN_KEY, value)
                .apply()

    override fun onCreate() {
        super.onCreate()

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        instance = this

        JodaTimeAndroid.init(this)

        Preferences.logLineDate("MyApplication.onCreate")

        FirebaseDatabase.getInstance().apply {
            setLogLevel(Logger.Level.DEBUG)
            setPersistenceEnabled(true)
        }

        FirebaseAuth.getInstance().addAuthStateListener { firebaseAuth: FirebaseAuth ->
            val firebaseUser = firebaseAuth.currentUser
            DomainFactory.getInstance().apply {
                if (firebaseUser != null) {
                    setUserInfo(SaveService.Source.GUI, UserInfo(firebaseUser))
                } else {
                    clearUserInfo()
                }
            }
        }

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
