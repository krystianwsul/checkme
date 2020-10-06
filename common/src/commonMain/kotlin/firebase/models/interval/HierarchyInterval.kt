package com.krystianwsul.common.firebase.models.interval

import com.krystianwsul.common.firebase.models.TaskHierarchy
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.Current
import com.krystianwsul.common.utils.ProjectType

class HierarchyInterval<T : ProjectType>(
        override val startExactTimeStamp: ExactTimeStamp,
        override val endExactTimeStamp: ExactTimeStamp?,
        val taskHierarchy: TaskHierarchy<T>
) : Current