package com.krystianwsul.checkme.firebase

import com.krystianwsul.checkme.firebase.loaders.Snapshot

sealed class DatabaseEvent {

    abstract val key: String

    class AddChange(val dataSnapshot: Snapshot) : DatabaseEvent() {

        override val key = dataSnapshot.key
    }

    class Remove(override val key: String) : DatabaseEvent()
}