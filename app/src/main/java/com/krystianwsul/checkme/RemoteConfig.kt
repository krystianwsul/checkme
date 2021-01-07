package com.krystianwsul.checkme

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.krystianwsul.checkme.utils.toSingle
import com.krystianwsul.treeadapter.getCurrentValue
import io.reactivex.rxjava3.core.Observable
import java.util.concurrent.TimeUnit

object RemoteConfig {

    private const val BENIA_URL_KEY = "beniaAboutUrl"
    private const val KEY_QUERY_REMOTE_INSTANCES = "queryRemoteInstance"

    private val config = FirebaseRemoteConfig.getInstance().apply {
        setDefaultsAsync(mapOf(
                BENIA_URL_KEY to "https://www.linkedin.com/in/bernardawsul/",
                KEY_QUERY_REMOTE_INSTANCES to false
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

        val queryRemoteInstances get() = config.getBoolean(KEY_QUERY_REMOTE_INSTANCES)
    }
}