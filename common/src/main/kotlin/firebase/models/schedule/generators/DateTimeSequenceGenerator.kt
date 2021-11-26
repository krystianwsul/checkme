package firebase.models.schedule.generators

import com.krystianwsul.common.time.DateTime
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.Time

interface DateTimeSequenceGenerator {

    fun generate(
        startExactTimeStamp: ExactTimeStamp,
        endExactTimeStamp: ExactTimeStamp?,
        scheduleTime: Time,
    ): Sequence<DateTime>
}