package com.krystianwsul.checkme.firebase.managers

import com.krystianwsul.checkme.firebase.loaders.ProjectProvider
import com.krystianwsul.checkme.firebase.loaders.Snapshot
import com.krystianwsul.common.domain.UserInfo
import com.krystianwsul.common.firebase.ChangeType
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.json.PrivateProjectJson
import com.krystianwsul.common.firebase.managers.PrivateProjectManager
import com.krystianwsul.common.firebase.records.PrivateProjectRecord
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.ProjectType

class AndroidPrivateProjectManager(
        private val userInfo: UserInfo,
        override val databaseWrapper: DatabaseWrapper
) : PrivateProjectManager(), ProjectProvider.ProjectManager<ProjectType.Private> {

    private lateinit var privateProjectRecord: PrivateProjectRecord

    override val privateProjectRecords get() = listOf(privateProjectRecord)

    private fun Snapshot.toRecord() = PrivateProjectRecord(
            databaseWrapper,
            userInfo.key.toPrivateProjectKey(),
            getValue(PrivateProjectJson::class.java)!!
    )

    private var first = true

    override fun setProjectRecord(snapshot: Snapshot): ChangeWrapper<PrivateProjectRecord> {
        return if (isSaved) {
            isSaved = false

            ChangeWrapper(ChangeType.LOCAL, privateProjectRecord)
        } else {
            privateProjectRecord = if (first) {
                first = false // for new users, the project may not exist yet

                snapshot.takeIf { it.exists() }
                        ?.toRecord()
                        ?: PrivateProjectRecord(
                                databaseWrapper,
                                userInfo,
                                PrivateProjectJson(startTime = ExactTimeStamp.now.long)
                        )
            } else {
                snapshot.toRecord()
            }

            ChangeWrapper(ChangeType.REMOTE, privateProjectRecord)
        }
    }
}