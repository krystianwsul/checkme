package com.krystianwsul.checkme.firebase.managers

import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.firebase.loaders.ProjectProvider
import com.krystianwsul.checkme.firebase.loaders.Snapshot
import com.krystianwsul.checkme.utils.MapRelayProperty
import com.krystianwsul.checkme.utils.checkError
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.json.JsonWrapper
import com.krystianwsul.common.firebase.managers.SharedProjectManager
import com.krystianwsul.common.firebase.records.SharedProjectRecord
import com.krystianwsul.common.firebase.records.TaskRecord
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ProjectType

class AndroidSharedProjectManager(
        children: Iterable<Snapshot>, // todo instances remove
        private val database: DatabaseWrapper
) : SharedProjectManager<DomainFactory>(), ProjectProvider.ProjectManager<ProjectType.Shared> {

    private fun Snapshot.toRecord() = SharedProjectRecord(
            database,
            this@AndroidSharedProjectManager,
            ProjectKey.Shared(key),
            getValue(JsonWrapper::class.java)!!
    )

    private val sharedProjectProperty = MapRelayProperty(
            this,
            children.mapNotNull {
                        try {
                            ProjectKey.Shared(it.key) to Pair(it.toRecord(), false)
                        } catch (onlyVisibilityPresentException: TaskRecord.OnlyVisibilityPresentException) {
                            MyCrashlytics.logException(onlyVisibilityPresentException)

                            null
                        }
                    }
                    .toMap()
    )

    override var sharedProjectRecords by sharedProjectProperty

    val sharedProjectObservable = sharedProjectProperty.observable.map {
        it.mapValues { it.value.first }
    }!!

    override val databaseWrapper = database

    override fun setProjectRecord(snapshot: Snapshot): SharedProjectRecord? {
        val key = ProjectKey.Shared(snapshot.key)
        val pair = sharedProjectRecords[key]

        if (pair?.second == true) {
            sharedProjectProperty[key] = Pair(pair.first, false)

            return null
        }

        return try {
            snapshot.toRecord().also {
                sharedProjectProperty[key] = Pair(it, false)
            }
        } catch (onlyVisibilityPresentException: TaskRecord.OnlyVisibilityPresentException) {
            MyCrashlytics.logException(onlyVisibilityPresentException)

            null
        }
    }

    override fun getDatabaseCallback(extra: DomainFactory) = checkError(extra, "RemoteSharedProjectManager.save")

    override fun setSharedProjectRecord(projectKey: ProjectKey.Shared, pair: Pair<SharedProjectRecord, Boolean>) {
        sharedProjectProperty[projectKey] = pair
    }

    override fun deleteRemoteSharedProjectRecord(projectKey: ProjectKey.Shared) {
        check(sharedProjectProperty.remove(projectKey) != null)
    }
}