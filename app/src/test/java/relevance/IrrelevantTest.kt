package relevance

import com.krystianwsul.common.domain.UserInfo
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.json.PrivateProjectJson
import com.krystianwsul.common.firebase.models.PrivateProject
import com.krystianwsul.common.firebase.models.Project
import com.krystianwsul.common.firebase.records.PrivateProjectRecord
import com.krystianwsul.common.relevance.Irrelevant
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.HourMinute
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

        val parent = mockk<Project.Parent>()

        val databaseWrapper = mockk<DatabaseWrapper>()
        val userInfo = spyk(UserInfo("email", "name")) {
            every { key } returns UserKey("key")
        }

        val projectJson = PrivateProjectJson(startTime = ExactTimeStamp(day1, hour1).long)
        val projectRecord = PrivateProjectRecord(databaseWrapper, userInfo, projectJson)

        val project = PrivateProject(projectRecord, mapOf()) { mockk() }

        Irrelevant.setIrrelevant(parent, project, ExactTimeStamp(day1, hour1))
    }
}