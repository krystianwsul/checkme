package com.krystianwsul.common.firebase.json.users

data class ProjectOrdinalKeyEntryJson @JvmOverloads constructor(
    val taskInfoJson: TaskInfoJson? = null,
    val instanceDateOrDayOfWeek: String = "",
    val instanceTime: String = "",
) {

    data class TaskInfoJson @JvmOverloads constructor(
        val taskKey: String = "",
        val scheduleDateTimePairJson: DateTimePairJson? = null,
    ) {

        data class DateTimePairJson @JvmOverloads constructor(val date: String = "", val time: String = "")
    }
}