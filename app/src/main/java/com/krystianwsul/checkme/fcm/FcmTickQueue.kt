package com.krystianwsul.checkme.fcm

import com.krystianwsul.checkme.ticks.Ticker

object FcmTickQueue {

    fun enqueue() {
        Ticker.tick("MyFirebaseMessagingService", true).subscribe()
    }
}