package com.krystianwsul.checkme.firebase.managers

import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.firebase.loaders.ProjectProvider
import com.krystianwsul.checkme.firebase.loaders.Snapshot
import com.krystianwsul.checkme.utils.checkError
import com.krystianwsul.common.firebase.ChangeType
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.json.JsonWrapper
import com.krystianwsul.common.firebase.managers.SharedProjectManager
import com.krystianwsul.common.firebase.records.SharedProjectRecord
import com.krystianwsul.common.firebase.records.TaskRecord
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ProjectType

class AndroidSharedProjectManager(private val database: DatabaseWrapper) :
        SharedProjectManager<DomainFactory>(),
        ProjectProvider.ProjectManager<ProjectType.Shared> {

    private fun Snapshot.toRecord() = SharedProjectRecord(
            database,
            this@AndroidSharedProjectManager,
            ProjectKey.Shared(key),
            getValue(JsonWrapper::class.java)!!
    )

    override val databaseWrapper = database

    override fun setProjectRecord(snapshot: Snapshot): ChangeWrapper<SharedProjectRecord>? {
        val key = ProjectKey.Shared(snapshot.key)
        val pair = sharedProjectRecords[key]

        return if (pair?.second == true) {
            sharedProjectRecords[key] = Pair(pair.first, false)

            ChangeWrapper(ChangeType.LOCAL, pair.first)
        } else {
            try {
                val sharedProjectRecord = snapshot.toRecord()

                sharedProjectRecords[key] = Pair(sharedProjectRecord, false)

                ChangeWrapper(ChangeType.REMOTE, sharedProjectRecord)
            } catch (onlyVisibilityPresentException: TaskRecord.OnlyVisibilityPresentException) {
                MyCrashlytics.logException(onlyVisibilityPresentException)

                null
            }
        }
    }

    override fun getDatabaseCallback(extra: DomainFactory) = checkError(extra, "RemoteSharedProjectManager.save")

    override fun setSharedProjectRecord(projectKey: ProjectKey.Shared, pair: Pair<SharedProjectRecord, Boolean>) {
        sharedProjectRecords[projectKey] = pair
    }

    override fun deleteRemoteSharedProjectRecord(projectKey: ProjectKey.Shared) {
        check(sharedProjectRecords.remove(projectKey) != null)
    }
}