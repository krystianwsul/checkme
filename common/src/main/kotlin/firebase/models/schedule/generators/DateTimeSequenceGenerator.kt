package firebase.models.schedule.generators

import com.krystianwsul.common.time.*

interface DateTimeSequenceGenerator {

    companion object {

        fun DateSoy.toDate() = Date(year, month1, day)
    }

    fun generate(
        startExactTimeStamp: ExactTimeStamp,
        endExactTimeStamp: ExactTimeStamp?,
        scheduleTime: Time,
    ): Sequence<DateTime>
}