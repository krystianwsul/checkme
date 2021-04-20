package com.krystianwsul.checkme

import android.annotation.SuppressLint
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.krystianwsul.checkme.utils.toSingle
import com.krystianwsul.treeadapter.getCurrentValue
import io.reactivex.rxjava3.core.Observable
import java.util.concurrent.TimeUnit

object RemoteConfig {

    private const val BENIA_URL_KEY = "beniaAboutUrl"

    @SuppressLint("StaticFieldLeak")
    private val config = FirebaseRemoteConfig.getInstance().apply {
        setDefaultsAsync(mapOf(
                BENIA_URL_KEY to "https://www.linkedin.com/in/bernardawsul/",
        ))
    }

    val observable = Observable.interval(0, 12, TimeUnit.HOURS)
            .switchMapSingle { config.fetchAndActivate().toSingle() }
            .filter { it.value == true }
            .map { }
            .startWithItem(Unit)
            .map { Values(config) }
            .replay(1)
            .apply { connect() }!!

    val value get() = observable.getCurrentValue()!!

    class Values(private val config: FirebaseRemoteConfig) {

        val beniaUrl get() = config.getString(BENIA_URL_KEY)
    }
}