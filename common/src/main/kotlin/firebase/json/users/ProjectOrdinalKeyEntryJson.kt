package com.krystianwsul.common.firebase.json.users

data class ProjectOrdinalKeyEntryJson @JvmOverloads constructor(
    val taskInfoJson: TaskInfoJson? = null,
    val instanceDateOrDayOfWeek: String = "", // todo ordinal use method from instanceJson, plus DayOfWeek.ordinal
    val instanceTime: String = "", // todo ordinal use method from instanceJson
) {

    // todo ordinal unit test this whole thing, round-trip, for variations

    data class TaskInfoJson @JvmOverloads constructor(
        val taskKey: String = "", // todo ordinal use method from Task.fromShortcut
        val dateTimePairJson: DateTimePairJson? = null,
    ) {

        data class DateTimePairJson @JvmOverloads constructor(
            val date: String = "", // todo ordinal use method from instanceJson
            val time: String = "", // todo ordinal use method from instanceJson
        )
    }
}