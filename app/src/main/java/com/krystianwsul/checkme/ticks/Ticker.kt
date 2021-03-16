package com.krystianwsul.checkme.ticks

import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.TickData
import com.krystianwsul.checkme.domainmodel.notifications.NotificationWrapper
import com.krystianwsul.checkme.domainmodel.observeOnDomain
import com.krystianwsul.checkme.persistencemodel.SaveService
import io.reactivex.rxjava3.core.Completable


object Ticker {

    const val TICK_NOTIFICATION_ID = 1

    fun tick(source: String, domainChanged: Boolean = false) {
        NotificationWrapper.instance.notifyTemporary(TICK_NOTIFICATION_ID, "Ticker.start $source")

        Preferences.tickLog.logLineDate("Ticker.tick from $source")
        Preferences.temporaryNotificationLog.logLineHour("Ticker.tick from $source")

        if (!MyApplication.instance.hasUserInfo) {
            Preferences.tickLog.logLineHour("Ticker.tick skipping, no userInfo")

            NotificationWrapper.instance.hideTemporary(TICK_NOTIFICATION_ID, "Ticker.tick skipping")
        } else {
            Completable.complete()
                    .observeOnDomain()
                    .subscribe {
                        DomainFactory.setFirebaseTickListener(
                                SaveService.Source.SERVICE,
                                TickData.Lock(source, domainChanged),
                        )
                    }
        }
    }
}