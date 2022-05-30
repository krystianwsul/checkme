package com.krystianwsul.checkme.firebase.managers

import com.krystianwsul.checkme.firebase.loaders.ProjectProvider
import com.krystianwsul.checkme.firebase.snapshot.Snapshot
import com.krystianwsul.common.domain.UserInfo
import com.krystianwsul.common.firebase.json.projects.PrivateOwnedProjectJson
import com.krystianwsul.common.firebase.managers.PrivateProjectManager
import com.krystianwsul.common.firebase.records.project.PrivateOwnedProjectRecord
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ProjectType

class AndroidPrivateProjectManager(private val userInfo: UserInfo) :
    PrivateProjectManager(),
    ProjectProvider.ProjectManager<ProjectType.Private, PrivateOwnedProjectJson, PrivateOwnedProjectRecord> {

    private fun Snapshot<out PrivateOwnedProjectJson>.toRecord() = PrivateOwnedProjectRecord(
        userInfo.key.toPrivateProjectKey(),
        value!!,
    )

    private var first = true

    override fun set(
        projectKey: ProjectKey<out ProjectType.Private>,
        snapshot: Snapshot<out PrivateOwnedProjectJson>
    ): PrivateOwnedProjectRecord? {
        val value = set(
            { it.single().createObject != snapshot.value },
            {
                val record = if (first) {
                    first = false // for new users, the project may not exist yet

                    val now = ExactTimeStamp.Local.now

                    snapshot.takeIf { it.exists }
                        ?.toRecord()
                        ?: PrivateOwnedProjectRecord(
                            userInfo,
                            PrivateOwnedProjectJson(userInfo.name, now.long, now.offset),
                        )
                } else {
                    snapshot.toRecord()
                }

                listOf(record)
            }
        )

        return value?.single()
    }
}