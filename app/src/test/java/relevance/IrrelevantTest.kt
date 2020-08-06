package relevance

import com.krystianwsul.common.domain.UserInfo
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.json.PrivateProjectJson
import com.krystianwsul.common.firebase.json.ScheduleWrapper
import com.krystianwsul.common.firebase.json.SingleScheduleJson
import com.krystianwsul.common.firebase.json.TaskJson
import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.firebase.models.PrivateProject
import com.krystianwsul.common.firebase.models.Project
import com.krystianwsul.common.firebase.records.PrivateProjectRecord
import com.krystianwsul.common.relevance.Irrelevant
import com.krystianwsul.common.time.*
import com.krystianwsul.common.utils.UserKey
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import org.junit.Test

class IrrelevantTest {

    @Test
    fun testDisappearingTask() {
        val day1 = Date(2020, 1, 1)
        val hour1 = HourMinute(1, 1).toHourMilli()
        val hour2 = HourMinute(2, 1).toHourMilli()
        val hour3 = HourMinute(3, 1).toHourMilli()
        val hour4 = HourMinute(4, 1)

        val parent = mockk<Project.Parent>()

        val databaseWrapper = mockk<DatabaseWrapper> {
            every { getPrivateTaskRecordId(any()) } returns "taskKey"
        }

        val userKey = UserKey("key")

        val userInfo = spyk(UserInfo("email", "name")) {
            every { key } returns userKey
        }

        var now = ExactTimeStamp(day1, hour1)

        val projectJson = PrivateProjectJson(startTime = now.long)
        val projectRecord = PrivateProjectRecord(databaseWrapper, userInfo, projectJson)

        val project = PrivateProject(projectRecord, mapOf()) {
            mockk {
                every { records } returns mutableListOf()
            }
        }

        now = ExactTimeStamp(day1, hour2)

        val scheduleWrapper = ScheduleWrapper(
                singleScheduleJson = SingleScheduleJson(
                        startTime = now.long,
                        year = day1.year,
                        month = day1.month,
                        day = day1.day,
                        hour = hour3.hour,
                        minute = hour3.minute
                )
        )

        val taskJson = TaskJson(
                name = "task",
                startTime = now.long,
                schedules = mutableMapOf("scheduleKey" to scheduleWrapper)
        )

        val task = project.newTask(taskJson)

        now = ExactTimeStamp(day1, hour3)

        val instance = task.getPastRootInstances(now).single()

        val shownFactory = mockk<Instance.ShownFactory> {
            every { getShown(any(), any()) } returns mockk(relaxed = true)
        }

        instance.setInstanceDateTime(shownFactory, userKey, DateTime(day1, Time.Normal(hour4)), now)

        Irrelevant.setIrrelevant(parent, project, now)
    }
}