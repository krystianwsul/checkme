package com.krystianwsul.checkme

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.krystianwsul.checkme.utils.getCurrentValue
import com.krystianwsul.checkme.utils.toSingle
import io.reactivex.Observable
import java.util.concurrent.TimeUnit

object RemoteConfig {

    private const val BENIA_URL_KEY = "beniaAboutUrl"

    private val config = FirebaseRemoteConfig.getInstance().apply {
        setDefaultsAsync(mapOf(BENIA_URL_KEY to "https://www.linkedin.com/in/bernardawsul/"))
    }

    val observable = Observable.interval(0, 12, TimeUnit.HOURS)
            .switchMapSingle { config.fetchAndActivate().toSingle() }
            .filter { it.value == true }
            .map { Unit }
            .startWith(Unit)
            .map { Values(config) }
            .replay(1)
            .apply { connect() }!!

    val value get() = observable.getCurrentValue()!!

    class Values(private val config: FirebaseRemoteConfig) {

        val beniaUrl get() = config.getString(BENIA_URL_KEY)
    }
}