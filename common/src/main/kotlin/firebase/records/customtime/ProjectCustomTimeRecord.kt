package com.krystianwsul.common.firebase.records.customtime

import com.krystianwsul.common.firebase.records.project.OwnedProjectRecord
import com.krystianwsul.common.utils.CustomTimeId
import com.krystianwsul.common.utils.CustomTimeKey
import com.krystianwsul.common.utils.ProjectType


abstract class ProjectCustomTimeRecord<T : ProjectType>(create: Boolean) : CustomTimeRecord(create) {

    abstract override val id: CustomTimeId.Project
    abstract override val customTimeKey: CustomTimeKey.Project<T>
    protected abstract val projectRecord: OwnedProjectRecord<T>

    val projectId get() = projectRecord.projectKey

    override val key get() = projectRecord.childKey + "/" + CUSTOM_TIMES + "/" + id

    override val name get() = customTimeJson.name

    override val sundayHour get() = customTimeJson.sundayHour
    override val sundayMinute get() = customTimeJson.sundayMinute

    override val mondayHour get() = customTimeJson.mondayHour
    override val mondayMinute get() = customTimeJson.mondayMinute

    override val tuesdayHour get() = customTimeJson.tuesdayHour
    override val tuesdayMinute get() = customTimeJson.tuesdayMinute

    override val wednesdayHour get() = customTimeJson.wednesdayHour
    override val wednesdayMinute get() = customTimeJson.wednesdayMinute

    override val thursdayHour get() = customTimeJson.thursdayHour
    override val thursdayMinute get() = customTimeJson.thursdayMinute

    override val fridayHour get() = customTimeJson.fridayHour
    override val fridayMinute get() = customTimeJson.fridayMinute

    override val saturdayHour get() = customTimeJson.saturdayHour
    override val saturdayMinute get() = customTimeJson.saturdayMinute
}
