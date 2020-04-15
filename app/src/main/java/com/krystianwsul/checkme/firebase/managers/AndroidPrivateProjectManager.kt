package com.krystianwsul.checkme.firebase.managers

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.firebase.loaders.ProjectProvider
import com.krystianwsul.checkme.firebase.loaders.Snapshot
import com.krystianwsul.checkme.utils.checkError
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
) : PrivateProjectManager<DomainFactory>(), ProjectProvider.ProjectManager<ProjectType.Private> {

    private lateinit var privateProjectRecord: PrivateProjectRecord

    override val privateProjectRecords get() = listOf(privateProjectRecord)

    private fun Snapshot.toRecord() = PrivateProjectRecord(
            databaseWrapper,
            userInfo.key.toPrivateProjectKey(),
            getValue(PrivateProjectJson::class.java)!!
    )

    private var first = true

    override fun setProjectRecord(snapshot: Snapshot): ChangeWrapper<PrivateProjectRecord> {
        if (isSaved) {
            isSaved = false

            return ChangeWrapper(ChangeType.LOCAL, privateProjectRecord)
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

            return ChangeWrapper(ChangeType.REMOTE, privateProjectRecord)
        }
    }

    override fun getDatabaseCallback(
            extra: DomainFactory,
            values: Map<String, Any?>
    ) = checkError(extra, "RemotePrivateProjectManager.save", values)
}