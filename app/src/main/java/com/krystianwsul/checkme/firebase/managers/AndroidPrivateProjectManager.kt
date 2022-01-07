package com.krystianwsul.checkme.firebase.managers

import com.krystianwsul.checkme.firebase.loaders.ProjectProvider
import com.krystianwsul.checkme.firebase.snapshot.Snapshot
import com.krystianwsul.common.domain.UserInfo
import com.krystianwsul.common.firebase.ChangeWrapper
import com.krystianwsul.common.firebase.json.projects.PrivateProjectJson
import com.krystianwsul.common.firebase.managers.PrivateProjectManager
import com.krystianwsul.common.firebase.records.project.PrivateProjectRecord
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.ProjectType

class AndroidPrivateProjectManager(private val userInfo: UserInfo) :
    PrivateProjectManager(), ProjectProvider.ProjectManager<ProjectType.Private, PrivateProjectJson> {

    private fun Snapshot<PrivateProjectJson>.toRecord() = PrivateProjectRecord(
        userInfo.key.toPrivateProjectKey(),
        value!!,
    )

    private var first = true

    override fun set(snapshot: Snapshot<PrivateProjectJson>): ChangeWrapper<PrivateProjectRecord>? {
        val changeWrapper = set(
                { it.single().createObject != snapshot.value },
                {
                    val record = if (first) {
                        first = false // for new users, the project may not exist yet

                        val now = ExactTimeStamp.Local.now

                        snapshot.takeIf { it.exists }
                                ?.toRecord()
                                ?: PrivateProjectRecord(
                                        userInfo,
                                        PrivateProjectJson(startTime = now.long, startTimeOffset = now.offset),
                                )
                    } else {
                        snapshot.toRecord()
                    }

                    listOf(record)
                }
        )

        return changeWrapper?.run { ChangeWrapper(changeType, data.single()) }
    }
}