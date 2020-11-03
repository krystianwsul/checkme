package com.krystianwsul.common.firebase.models.interval

import com.krystianwsul.common.firebase.models.TaskHierarchy
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.CurrentOffset
import com.krystianwsul.common.utils.ProjectType

class HierarchyInterval<T : ProjectType>(
        override val startExactTimeStampOffset: ExactTimeStamp,
        override val endExactTimeStampOffset: ExactTimeStamp?,
        val taskHierarchy: TaskHierarchy<T>
) : CurrentOffset