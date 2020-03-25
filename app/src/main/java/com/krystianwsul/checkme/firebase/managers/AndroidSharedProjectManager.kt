package com.krystianwsul.checkme.firebase.managers

import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.FactoryProvider
import com.krystianwsul.checkme.utils.MapRelayProperty
import com.krystianwsul.checkme.utils.checkError
import com.krystianwsul.common.firebase.json.JsonWrapper
import com.krystianwsul.common.firebase.managers.SharedProjectManager
import com.krystianwsul.common.firebase.records.SharedProjectRecord
import com.krystianwsul.common.firebase.records.TaskRecord
import com.krystianwsul.common.utils.ProjectKey

class AndroidSharedProjectManager(
        children: Iterable<FactoryProvider.Database.Snapshot>,
        private val factoryProvider: FactoryProvider
) : SharedProjectManager<DomainFactory>() {

    private fun FactoryProvider.Database.Snapshot.toRecord() = SharedProjectRecord(
            factoryProvider.database,
            this@AndroidSharedProjectManager,
            ProjectKey.Shared(key!!),
            getValue(JsonWrapper::class.java)!!
    )

    private val sharedProjectProperty = MapRelayProperty(
            this,
            children.mapNotNull {
                        try {
                            ProjectKey.Shared(it.key!!) to Pair(it.toRecord(), false)
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

    override val databaseWrapper = factoryProvider.database

    fun setChild(dataSnapshot: FactoryProvider.Database.Snapshot): SharedProjectRecord {
        val key = ProjectKey.Shared(dataSnapshot.key!!)

        return dataSnapshot.toRecord().also {
            sharedProjectProperty[key] = Pair(it, false)
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