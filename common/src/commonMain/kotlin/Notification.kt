package com.krystianwsul.common

class Notification(val registration_ids: List<String>) {

    val data = Data()
    val priority = "high"

    class Data {

        val refresh = true
    }
}
