package firebase.models.schedule.generators

import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.DateSoy
import com.krystianwsul.common.time.DateTime
import com.krystianwsul.common.time.ExactTimeStamp

interface DateTimeSequenceGenerator {

    companion object {

        fun DateSoy.toDate() = Date(year, month1, day)
    }

    fun generate(startExactTimeStamp: ExactTimeStamp, endExactTimeStamp: ExactTimeStamp?): Sequence<DateTime>
}