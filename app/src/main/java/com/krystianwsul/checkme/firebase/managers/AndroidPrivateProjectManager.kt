package com.krystianwsul.checkme.firebase.managers

import com.krystianwsul.checkme.firebase.loaders.ProjectProvider
import com.krystianwsul.checkme.firebase.snapshot.TypedSnapshot
import com.krystianwsul.common.domain.UserInfo
import com.krystianwsul.common.firebase.ChangeWrapper
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.json.PrivateProjectJson
import com.krystianwsul.common.firebase.managers.PrivateProjectManager
import com.krystianwsul.common.firebase.records.PrivateProjectRecord
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.ProjectType

class AndroidPrivateProjectManager(
        private val userInfo: UserInfo,
        override val databaseWrapper: DatabaseWrapper,
) : PrivateProjectManager(), ProjectProvider.ProjectManager<ProjectType.Private, PrivateProjectJson> {

    override lateinit var value: List<PrivateProjectRecord>

    private fun TypedSnapshot<PrivateProjectJson>.toRecord() = PrivateProjectRecord(
            databaseWrapper,
            userInfo.key.toPrivateProjectKey(),
            getValue()!!,
    )

    private var first = true

    override fun set(snapshot: TypedSnapshot<PrivateProjectJson>): ChangeWrapper<PrivateProjectRecord> {
        val changeWrapper = set {
            val record = if (first) {
                first = false // for new users, the project may not exist yet

                val now = ExactTimeStamp.Local.now

                snapshot.takeIf { it.exists() }
                        ?.toRecord()
                        ?: PrivateProjectRecord(
                                databaseWrapper,
                                userInfo,
                                PrivateProjectJson(
                                        startTime = now.long,
                                        startTimeOffset = now.offset
                                )
                        )
            } else {
                snapshot.toRecord()
            }

            listOf(record)
        }

        return ChangeWrapper(changeWrapper.changeType, changeWrapper.data.single())
    }
}