package firebase.models.interval

import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.ProjectType

sealed class Interval<T : ProjectType> {

    abstract val type: Type<T>

    abstract val startExactTimeStamp: ExactTimeStamp
    abstract val endExactTimeStamp: ExactTimeStamp?

    open fun containsExactTimeStamp(exactTimeStamp: ExactTimeStamp) = startExactTimeStamp <= exactTimeStamp

    data class Current<T : ProjectType>(
            override val type: Type<T>,
            override val startExactTimeStamp: ExactTimeStamp
    ) : Interval<T>() {

        override val endExactTimeStamp: ExactTimeStamp? = null
    }

    data class Ended<T : ProjectType>(
            override val type: Type<T>,
            override val startExactTimeStamp: ExactTimeStamp,
            override val endExactTimeStamp: ExactTimeStamp
    ) : Interval<T>() {

        override fun containsExactTimeStamp(exactTimeStamp: ExactTimeStamp): Boolean {
            if (!super.containsExactTimeStamp(exactTimeStamp))
                return false

            return endExactTimeStamp > exactTimeStamp
        }
    }
}