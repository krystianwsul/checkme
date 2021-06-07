package com.krystianwsul.checkme.domainmodel.notifications

import com.krystianwsul.common.time.TimeStamp
import com.krystianwsul.common.utils.ProjectKey

data class ProjectNotificationKey(val projectKey: ProjectKey.Shared, val timeStamp: TimeStamp)