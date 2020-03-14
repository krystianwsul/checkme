package com.krystianwsul.checkme.firebase

import com.google.firebase.database.DataSnapshot

sealed class DatabaseEvent {

    abstract val key: String

    class AddChange(val dataSnapshot: DataSnapshot) : DatabaseEvent() {

        override val key = dataSnapshot.key!!
    }

    class Remove(override val key: String) : DatabaseEvent()
}