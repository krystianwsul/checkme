package firebase.models.schedule.generators

import com.krystianwsul.common.FeatureFlagManager
import com.krystianwsul.common.time.DateTime
import com.krystianwsul.common.time.ExactTimeStamp

class ProxyDateTimeSequenceGenerator(
    private val oldDateTimeSequenceGenerator: DateTimeSequenceGenerator,
    private val newDateTimeSequenceGenerator: DateTimeSequenceGenerator,
    private val flag: FeatureFlagManager.Flag,
) : DateTimeSequenceGenerator {

    override fun generate(startExactTimeStamp: ExactTimeStamp, endExactTimeStamp: ExactTimeStamp?): Sequence<DateTime> {
        val generator = if (FeatureFlagManager.getFlag(flag)) {
            newDateTimeSequenceGenerator
        } else {
            oldDateTimeSequenceGenerator
        }

        return generator.generate(startExactTimeStamp, endExactTimeStamp)
    }
}