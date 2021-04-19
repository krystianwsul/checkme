package com.krystianwsul.common.firebase.models.interval

import com.krystianwsul.common.firebase.models.ProjectTaskHierarchy
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.CurrentOffset
import com.krystianwsul.common.utils.ProjectType

class HierarchyInterval<T : ProjectType>(
        override val startExactTimeStampOffset: ExactTimeStamp.Offset,
        override val endExactTimeStampOffset: ExactTimeStamp.Offset?,
        val taskHierarchy: ProjectTaskHierarchy<T>,
) : CurrentOffset