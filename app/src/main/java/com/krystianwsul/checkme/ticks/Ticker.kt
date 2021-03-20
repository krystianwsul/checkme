package com.krystianwsul.checkme.ticks

import androidx.annotation.CheckResult
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.TickData
import com.krystianwsul.checkme.domainmodel.notifications.NotificationWrapper
import io.reactivex.rxjava3.core.Completable


object Ticker {

    const val TICK_NOTIFICATION_ID = 1

    @CheckResult
    fun tick(source: String, domainChanged: Boolean = false): Completable {
        NotificationWrapper.instance.notifyTemporary(TICK_NOTIFICATION_ID, "Ticker.start $source")

        Preferences.tickLog.logLineDate("Ticker.tick from $source")
        Preferences.temporaryNotificationLog.logLineHour("Ticker.tick from $source")

        return if (!MyApplication.instance.hasUserInfo) {
            Preferences.tickLog.logLineHour("Ticker.tick skipping, no userInfo")

            NotificationWrapper.instance.hideTemporary(TICK_NOTIFICATION_ID, "Ticker.tick skipping")

            Completable.complete()
        } else {
            DomainFactory.setFirebaseTickListener(TickData.Lock(source, domainChanged))
        }
    }
}